package com.yhaitao.mark.chinaz;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.yhaitao.mark.chinaz.bean.ChinazWeb;
import com.yhaitao.mark.http.MarkHttpClient;

/**
 * 运行命令： nohup java -jar mark-chinaz.jar /home/app/jobs/mark-chinaz/chinazWebInfo.txt & 
 * @author Administrator
 *
 */
public class ChinazCrawler {
	public final static Gson GSON = new Gson();
	private final static Logger LOGGER = LoggerFactory.getLogger(ChinazCrawler.class);
	public static void main(String[] args) throws Exception {
		Map<String, String> map = readOriginalFile();
		FileUtils.write(new File(args[0]), GSON.toJson(map), "UTF-8", true);
	}
	
	/**
	 * 原始数据文件。
	 * @return key文本标识，value文本内容
	 */
	public static Map<String, String> readOriginalFile() {
		Map<String, String> dataMap = new HashMap<String, String>();
		MarkHttpClient httpClient = new MarkHttpClient();
		String baseUrl = "http://top.chinaz.com/hangye/";
		for(int i = 1; i <= 1881; i++) {
			String crawlerUrl = (i == 1) ? baseUrl : (baseUrl + "index_" + i + ".html");
			try {
				String response = httpClient.httpGet(crawlerUrl);
				List<ChinazWeb> filterList = filterList(response);
				dataMap.putAll(outdata(filterList));
				LOGGER.info("httpGet : {}, success. ", crawlerUrl);
			} catch (Exception e) {
				LOGGER.warn("httpGet : {}, Exception : {}. ", crawlerUrl, e.getMessage());
				continue ;
			}
		}
		return dataMap;
	}
	
	/**
	 * 组装输出数据
	 * @param filterList
	 * @return
	 */
	private static Map<String, String> outdata(List<ChinazWeb> filterList) {
		Map<String, String> dataMap = new HashMap<String, String>();
		for(ChinazWeb web : filterList) {
			dataMap.put(web.getDomain(), web.getName() + " " + web.getDesc().replace("网站简介：", ""));
		}
		return dataMap;
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

}
