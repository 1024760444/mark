package com.yhaitao.mark.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * 
 * @author yhaitao
 *
 */
public class MarkHttpClient {

	/**
	 * HTTP请求客户端。
	 */
	private HttpClient httpClient;

	/**
	 * 网页请求时间，单位毫秒。
	 */
	private long timeOut;

	/**
	 * 初始化客户端。
	 */
	public MarkHttpClient() {
		httpClient = HttpClients.createDefault();
	}

	/**
	 * 爬取地址的网页内容。
	 * 
	 * @param url
	 *            网页地址
	 * @return 网页文本内容
	 * @throws Exception
	 *             爬取异常，或域名不存在、或请求超时、或协议错误等。
	 */
	public String httpGet(String url, int timeOut) throws Exception {
		this.timeOut = 0;
		HttpGet get = new HttpGet(url);
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeOut / 2)
				.setConnectTimeout(timeOut / 2).build();
		get.setConfig(requestConfig);
		long start = System.currentTimeMillis();
		HttpResponse httpResponse = httpClient.execute(get);
		this.timeOut = System.currentTimeMillis() - start;
		return parse(httpResponse);
	}

	/**
	 * 获取指定url的文件。
	 * 
	 * @param url
	 *            scel文件地址
	 * @param timeOut
	 *            超时时间
	 * @throws Exception
	 */
	public void httpGet(String url, String filePath, int timeOut) throws Exception {
		this.timeOut = 0;
		HttpGet get = new HttpGet(url);
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeOut / 2)
				.setConnectTimeout(timeOut / 2).build();
		get.setConfig(requestConfig);
		long start = System.currentTimeMillis();
		HttpResponse httpResponse = httpClient.execute(get);
		this.timeOut = System.currentTimeMillis() - start;
		parse(httpResponse, filePath);
	}

	/**
	 * 爬取地址的网页内容， 默认超时2000毫秒。
	 * 
	 * @param url
	 *            网页地址
	 * @return 网页文本内容
	 * @throws Exception
	 *             爬取异常，或域名不存在、或请求超时、或协议错误等
	 */
	public String httpGet(String url) throws Exception {
		return httpGet(url, 24000);
	}

	/**
	 * 获取请求响应时间。
	 * 
	 * @return 响应时间，单位毫秒
	 */
	public long getTimeOut() {
		return this.timeOut;
	}

	/**
	 * 解析网页响应。
	 * 
	 * @param httpResponse
	 *            网页响应
	 * @return 网页文本
	 * @throws IOException
	 * @throws ParseException
	 */
	private String parse(HttpResponse httpResponse) throws ParseException, IOException {
		int statusCode = httpResponse.getStatusLine().getStatusCode();
		String response = null;
		if (statusCode == HttpStatus.SC_OK) {
			response = EntityUtils.toString(httpResponse.getEntity());
			if (response.contains("charset=gb2312") || response.contains("charset=GB2312")) {
				response = new String(response.getBytes("ISO-8859-1"), "GBK");
			}
			if (response.contains("charset=gbk") || response.contains("charset=GBK")) {
				response = new String(response.getBytes("ISO-8859-1"), "GBK");
			}
			if (response.contains("charset=utf-8") || response.contains("charset=UTF-8")) {
				response = new String(response.getBytes("ISO-8859-1"), "utf-8");
			}
		}
		return response;
	}

	/**
	 * 
	 * @param httpResponse
	 * @throws ParseException
	 * @throws IOException
	 */
	private void parse(HttpResponse httpResponse, String filePath) throws ParseException, IOException {
		// 如果响应为空，
		if (httpResponse == null) {
			return;
		}

		// 获取文件输入流。
		HttpEntity entity = httpResponse.getEntity();
		OutputStream outputStream = new FileOutputStream(new File(filePath));
		if (entity != null) {
			// 获取输入流
			InputStream content = entity.getContent();
			int temp = 0;
			while ((temp = content.read()) != -1) { // 当没有读取完时，继续读取
				outputStream.write(temp);
			}
			outputStream.flush();
			outputStream.close();
			content.close();
		}
	}
}
