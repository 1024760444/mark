package com.yhaitao.mark;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.Writer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.MRJobConfig;
import org.apache.hadoop.util.StringUtils;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.vectorizer.SparseVectorsFromSequenceFiles;

/**
 * Kmean聚类实现。
 * @author yhaitao
 *
 */
public class KMeansCluster {
	public static void main(String[] args) throws Exception {
		// 参数解析，获取参数
		CommandLine commands = getCommandLine(args);
		if(commands == null) {
			return ;
		}
		String original = commands.getOptionValue("original");
		String in = commands.getOptionValue("in");
		String out = commands.getOptionValue("out");
		String extJars = commands.getOptionValue("extJars");
		
		// 参数设置
		Configuration conf = new Configuration();
		URI[] uriArray = getExtJars(conf, extJars);
		String sfiles = StringUtils.uriToString(uriArray);
		System.err.println("---------------- step 0 ---------------- sfiles : " + sfiles);
		conf.set(MRJobConfig.CACHE_FILES, sfiles);
		
		// 数据写入HDFS
		System.err.println("---------------- step 1 ----------------");
		writeToSequenceFile(conf, original, in);
		
		// 生成向量
		System.err.println("---------------- step 2 ----------------");
		sequenceToSparse(conf, in, out);
	}
	
	/**
	 * 生成向量
	 * @param in
	 * @param out
	 * @throws Exception 
	 */
	private static void sequenceToSparse(Configuration conf, String in, String out) throws Exception {
		String[] args = {
				"-i", in,
				"-o", out,
				"-lnorm", // 归一化
				"-nv", 
				"-ow", // 已有输出，覆盖
				"-wt", "tfidf", // 计算权重
				"-a", "org.wltea.analyzer.lucene.IKAnalyzer" // IK分词器
		};
		ToolRunner.run(conf, new SparseVectorsFromSequenceFiles(), args);
	}
	
	/**
	 * 获取第三方jar包的路径。
	 * @param conf 集群配置
	 * @param extJarsPath 第三方Jar包路径
	 * @return 路径
	 * @throws IOException
	 */
	private static URI[] getExtJars(Configuration conf, String extJarsPath) throws IOException {
		// 读取hdfs文件，获取jar列表
		FileSystem fileSystem = FileSystem.get(conf);
		RemoteIterator<LocatedFileStatus> listFiles = fileSystem.listFiles(new Path(extJarsPath), false);
		List<URI> uriList = new ArrayList<URI>();
		while(listFiles.hasNext()) {
			LocatedFileStatus next = listFiles.next();
			uriList.add(next.getPath().toUri());
		}
		fileSystem.close();
		URI[] array = new URI[uriList.size()];
		return uriList.toArray(array);
	}
	
	/**
	 * 
	 * @param original
	 * @param in
	 * @throws IOException 
	 */
	private static void writeToSequenceFile(Configuration conf, String original, String in) throws IOException {
		// SequenceFile写入工具
		Path path = new Path(in + "/original-seqdir/part-m-00000");
		SequenceFile.Writer writer = SequenceFile.createWriter(
				conf, 
				new SequenceFile.Writer.Option[]{
						SequenceFile.Writer.file(path),
						Writer.keyClass(Text.class),
						Writer.valueClass(Text.class)
				});
		
		// 写入数据
		Map<String, String> map = KMeansCluster.readOriginalFile(original);
		for(String key : map.keySet()) {
			String value = map.get(key);
			writer.append(new Text(key), new Text(value));
		}
		writer.close();
	}
	
	/**
	 * 读取原始数据。
	 * @param original 原始数据目录
	 * @return 原始数据键值对列表
	 * @throws IOException 
	 */
	private static Map<String, String> readOriginalFile(String original) throws IOException {
		File file = new File(original);
		BufferedReader br = new BufferedReader(new FileReader(file));
		String s = null;
		Map<String, String> data = new HashMap<String, String>();
		while ((s = br.readLine()) != null) {
			// 使用readLine方法，一次读一行
			String[] split = s.split(" ");
			if(split != null && split.length >= 2) {
				data.put(split[0], split[1]);
			}
		}
		br.close();
		return data;
	}

	/**
	 * 入参校验
	 * @param args 入参
	 * @throws ParseException 
	 */
	private static CommandLine getCommandLine(String[] args) throws ParseException {
		Options options = buildOptions();
		BasicParser parser = new BasicParser();
		CommandLine commands = parser.parse(options, args);
		if(!commands.hasOption("in")
				|| !commands.hasOption("out")
				|| !commands.hasOption("original")
				|| !commands.hasOption("extJars")) {
			printUsage(options);
			return null;
		} else {
			return commands;
		}
	}
	
	/**
	 * 输入信息提示。
	 * @return 需要输入数据的说明
	 */
	private static Options buildOptions() {
		Options options = new Options();
		options.addOption("in", true, "[required] HDFS path for kmeans input");
		options.addOption("out", true, "[required] HDFS path for kmeans out");
		options.addOption("original", true, "[required] original file path");
		options.addOption("extJars", true, "[required] HDFS path ext jars");
		return options;
	}
	
	/**
	 * 打印输入信息。
	 * @param options 打印输入帮助信息。
	 */
	public static void printUsage(Options options) {
		HelpFormatter help = new HelpFormatter();
		help.printHelp("Job of KMeansCluster need Params : ", options);
	}
}
