package com.yhaitao.mark.chinaz.bean;

public class ChinazWeb {
	/**
	 * 网站编号
	 */
	private String id;

	/**
	 * 网站名称
	 */
	private String name;

	/**
	 * 网站描述
	 */
	private String desc;

	/**
	 * 网站域名
	 */
	private String domain;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}
}