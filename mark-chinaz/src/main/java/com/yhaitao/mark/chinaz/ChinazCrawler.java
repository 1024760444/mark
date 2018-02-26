package com.yhaitao.mark.chinaz;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wltea.analyzer.lucene.IKAnalyzer;

import com.yhaitao.mark.chinaz.bean.ChinazWeb;
import com.yhaitao.mark.http.MarkHttpClient;

/**
 * 运行命令： nohup java -jar mark-chinaz.jar /home/app/jobs/mark-chinaz/chinazWebInfo.txt & 
 * @author Administrator
 *
 */
public class ChinazCrawler {
	/**
	 * 日志对象
	 */
	private final static Logger LOGGER = LoggerFactory.getLogger(ChinazCrawler.class);
	private static final MarkHttpClient httpClient = new MarkHttpClient();
	private static final Map<String, String> DOMAINMAP = new HashMap<String, String>();
	
	public static void main(String[] args) throws Exception {
		String baseUrl = "http://top.chinaz.com/hangye/";
		for(int i = 1; i <= 1881; i++) {
			String crawlerUrl = (i == 1) ? baseUrl : (baseUrl + "index_" + i + ".html");
			try {
				String response = httpClient.httpGet(crawlerUrl);
				List<ChinazWeb> filterList = filterList(response);
				String outdata = outdata(filterList);
				FileUtils.write(new File(args[0] + "/webdata.txt"), outdata, "UTF-8", true);
				LOGGER.info("success crawlerUrl : " + crawlerUrl);
			} catch (Exception e) {
				try {
					String response = httpClient.httpGet(crawlerUrl);
					List<ChinazWeb> filterList = filterList(response);
					String outdata = outdata(filterList);
					FileUtils.write(new File(args[0] + "/webdata.txt"), outdata, "UTF-8", true);
					LOGGER.info("success crawlerUrl : " + crawlerUrl);
				} catch (Exception e1) {
					LOGGER.error("error crawlerUrl : " + crawlerUrl + ", Exception : " + e.getMessage());
				}
			}
		}
		
		// 记录域名
		FileUtils.write(new File(args[0] + "/domainInfo.txt"), getDomainIdList(), "UTF-8", true);
	}
	
	/**
	 * 组装输出数据
	 * @param filterList
	 * @return
	 */
	private static String outdata(List<ChinazWeb> filterList) {
		StringBuffer outData = new StringBuffer();
		for(ChinazWeb web : filterList) {
			try {
				if(outData.length() > 0) {
					outData.append("\n");
				}
				List<String> termList = analyzer(web.getName() + " " + web.getDesc().replace("网站简介：", ""));
				outData.append(getDomainId(web.getDomain())).append(" ");
				for(String term : termList) {
					outData.append(term).append(" ");
				}
			} catch (IOException e) {}
		}
		return outData.toString();
	}
	
	/**
	 * 根据域名，获取域名的编号。
	 * @param domain 域名
	 * @return 域名编号
	 */
	private static String getDomainId(String domain) {
		String domainId = null;
		if(DOMAINMAP.containsKey(domain)) {
			domainId = DOMAINMAP.get(domain);
		} else {
			domainId = String.valueOf(1000000 + DOMAINMAP.size());
			DOMAINMAP.put(domain, domainId);
		}
		return domainId;
	}
	
	/**
	 * 输出域名编号字符串。
	 * @return 域名编号字符串，便于输出
	 */
	private static String getDomainIdList() {
		StringBuffer domainIdList = new StringBuffer();
		Iterator<Entry<String, String>> iterator = DOMAINMAP.entrySet().iterator();
		while(iterator.hasNext()) {
			Entry<String, String> next = iterator.next();
			String key = next.getKey();
			String value = next.getValue();
			if(domainIdList.length() > 0) {
				domainIdList.append("\n");
			}
			domainIdList.append(value).append(" ").append(key);
		}
		return domainIdList.toString();
	}
	
	/**
	 * 获取网页的标题。
	 * @param context 网页源码
	 * @return 网页的标题
	 */
	private static List<ChinazWeb> filterList(String context) {
		List<ChinazWeb> webList = new ArrayList<ChinazWeb>();
		Pattern pa = Pattern.compile("<div class=\"CentTxt\">(.*?)<div class=\"RtCRateWrap\">");
		Matcher ma = pa.matcher(context);
		while (ma.find()) {
			String webContext = ma.group(1);
			String name = filter(webContext, "title='(.*?)'");
			String desc = filter(webContext, "<p class=\"RtCInfo\">(.*?)</p>");
			String domain = filter(webContext, "<span class=\"col-gray\">(.*?)</span>");
			ChinazWeb web = new ChinazWeb();
			web.setName(name);
			web.setDesc(desc);
			web.setDomain(domain);
			webList.add(web);
		}
		return webList;
	}
	
	/**
	 * 正则匹配网页内容。
	 * @param context
	 * @param regex
	 * @return
	 */
	private static String filter(String context, String regex) {
		Pattern pa = Pattern.compile(regex);
		Matcher ma = pa.matcher(context);
        String title = null;
        while (ma.find()) {  
        	title = ma.group(1);
            if(title != null && !"".equals(title)) {
            	break;
            }
        }
		return title;
	}
	
	/**
	 * IK分词。
	 * @param context 输入文本
	 * @return 分词列表
	 * @throws IOException 
	 */
	private static List<String> analyzer(String context) throws IOException {
		IKAnalyzer iKAnalyzer = new IKAnalyzer();
		StringReader reader = new StringReader(context);
		TokenStream tokenStream = iKAnalyzer.tokenStream("", reader);
		List<String> termList = new ArrayList<String>();
		while(tokenStream.incrementToken()) {
			CharTermAttribute termAttri = tokenStream.getAttribute(CharTermAttribute.class);
			termList.add(termAttri.toString());
		}
		reader.close();
		iKAnalyzer.close();
		return termList;
	}
}
