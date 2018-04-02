package com.yhaitao.mark;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
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

import com.google.gson.Gson;

/**
 * Kmean聚类实现。
 * @author yhaitao
 *
 */
public class KMeansCluster {
	public final static Gson GSON = new Gson();
	
	/**
	 * 任务入口。
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		// 参数解析，获取参数
		CommandLine commands = getCommandLine(args);
		if(commands == null) {
			return ;
		}
		String in = commands.getOptionValue("in");
		String extJars = commands.getOptionValue("extJars");
		String local = commands.getOptionValue("local");
		
		// 参数设置
		Configuration conf = new Configuration();
		URI[] uriArray = getExtJars(conf, extJars);
		String sfiles = StringUtils.uriToString(uriArray);
		conf.set(MRJobConfig.CACHE_FILES, sfiles);
		
		Map<String, String> map = KMeansCluster.readOriginalFile(local);
		
		// 数据写入HDFS
		writeToSequenceFile(conf, in, map);
		
		// 生成向量
		// sequenceToSparse(conf, in, out);
	}
	
	@SuppressWarnings("unchecked")
	private static Map<String, String> readOriginalFile(String local) throws IOException {
		InputStreamReader reader = new InputStreamReader(new FileInputStream(local));
		BufferedReader br = new BufferedReader(reader);
		String line = br.readLine();
		Map<String, String> fromJson = GSON.fromJson(line, Map.class);
		br.close();
		reader.close();
		return fromJson;
	}

	/**
	 * 生成向量
	 * @param in
	 * @param out
	 * @throws Exception 
	 */
	public static void sequenceToSparse(Configuration conf, String in, String out) throws Exception {
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
	private static void writeToSequenceFile(Configuration conf, String in, Map<String, String> map) throws IOException {
		// SequenceFile写入工具
		Path path = new Path(in + "/part-m-00000");
		SequenceFile.Writer writer = SequenceFile.createWriter(
				conf, 
				new SequenceFile.Writer.Option[]{
						SequenceFile.Writer.file(path),
						Writer.keyClass(Text.class),
						Writer.valueClass(Text.class)
				});
		
		// 写入数据
		for(String key : map.keySet()) {
			String value = map.get(key);
			writer.append(new Text(key), new Text(value));
		}
		writer.close();
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
				|| !commands.hasOption("local")
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
		options.addOption("local", true, "[required] local data path");
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
