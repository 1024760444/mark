package com.yhaitao.mark.chinaz;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yhaitao.mark.chinaz.bean.ChinazWeb;
import com.yhaitao.mark.http.MarkHttpClient;

public class ChinazCrawler {
	/**
	 * 日志对象
	 */
	private final static Logger LOGGER = LoggerFactory.getLogger(ChinazCrawler.class);
	private static final MarkHttpClient httpClient = new MarkHttpClient();
	public static void main(String[] args) throws Exception {
		String baseUrl = "http://top.chinaz.com/hangye/";
		for(int i = 1; i <= 1881; i++) {
			String crawlerUrl = (i == 1) ? baseUrl : (baseUrl + "index_" + i + ".html");
			try {
				String response = httpClient.httpGet(crawlerUrl);
				List<ChinazWeb> filterList = filterList(response);
				String outdata = outdata(filterList);
				FileUtils.write(new File(args[0]), outdata, "UTF-8", true);
				LOGGER.info("success crawlerUrl : " + crawlerUrl);
			} catch (Exception e) {
				try {
					String response = httpClient.httpGet(crawlerUrl);
					List<ChinazWeb> filterList = filterList(response);
					String outdata = outdata(filterList);
					FileUtils.write(new File(args[0]), outdata, "UTF-8", true);
					LOGGER.info("success crawlerUrl : " + crawlerUrl);
				} catch (Exception e1) {
					LOGGER.error("error crawlerUrl : " + crawlerUrl + ", Exception : " + e.getMessage());
				}
			}
		}
	}
	
	/**
	 * 组装输出数据
	 * @param filterList
	 * @return
	 */
	public static String outdata(List<ChinazWeb> filterList) {
		StringBuffer outData = new StringBuffer();
		for(ChinazWeb web : filterList) {
			if(outData.length() > 0) {
				outData.append("\n");
			}
			outData.append(web.getDomain()).append(" ").append(web.getName()).append(" ").append(web.getDesc().replace("网站简介：", ""));
		}
		return outData.toString();
	}
	
	/**
	 * 获取网页的标题。
	 * @param context 网页源码
	 * @return 网页的标题
	 */
	public static List<ChinazWeb> filterList(String context) {
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
	public static String filter(String context, String regex) {
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
