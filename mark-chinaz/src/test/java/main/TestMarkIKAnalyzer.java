package main;

import java.io.IOException;

public class TestMarkIKAnalyzer {
	public static void main(String[] args) throws IOException {
		String s = "1000061 天气网 ★天气网（www.tianqi.com）★提供全国及世界各大城市天气预报查询以及历史天气查询，实时更新天气，准确提供天气预报一周查询及未来天气预报10天、15天、一个月查询服务。...";
		String[] split = s.split(" ");
		System.err.println(split[0] + "\n" + s.substring(split[0].length()));
	}
}
