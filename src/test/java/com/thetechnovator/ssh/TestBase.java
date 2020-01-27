package com.thetechnovator.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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
			InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
			properties.load(is);
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
