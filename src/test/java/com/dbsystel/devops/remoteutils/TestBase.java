package com.dbsystel.devops.remoteutils;

import java.io.IOException;
import java.util.Properties;

import com.dbsystel.devops.java.common.utils.FileUtil;

public class TestBase {
	protected static Properties properties;
	protected static String propertyFile="default.properties";
	protected static String USERNAME;
	protected static String SU_USERNAME;
	protected static String HOST;
	protected static String PASSWORD;
	protected static String SU_PASSWORD;
	
	public static void loadProperties(String fileName) {
		properties = new Properties();
		try {
			properties.load(FileUtil.getInputStemFromClasspathResource(fileName));
			HOST=properties.getProperty("host");
			USERNAME=properties.getProperty("username");
			PASSWORD=properties.getProperty("password");
			SU_USERNAME=properties.getProperty("su.username");
			SU_PASSWORD=properties.getProperty("su.password");
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	
	}
	public static void loadProperties() {
		loadProperties(propertyFile);
	}
	
	public void println(String msg) {
		System.out.println(msg);
	}
}
