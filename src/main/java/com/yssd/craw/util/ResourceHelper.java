package com.yssd.craw.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.commons.lang3.StringUtils;
/**
 * 
 * 加载resource配置文件类
 * 
 */
public class ResourceHelper {
	private static Properties prop;
	private static InputStream inputStream;
	private static ServletContext sc; 
	
	static {
		try {
			inputStream = ResourceHelper.class
					.getResourceAsStream("/config.properties");
			if(inputStream == null){
			    inputStream = ClassLoader.getSystemResourceAsStream( "config.properties" );
			}
			prop = new Properties();
			prop.load(inputStream);
			
		} catch (Exception ex) {
			try {
				if (inputStream!=null) {
					inputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			ex.printStackTrace();
		}
	}
	public static String get(String key){
	    return prop.getProperty(key);
	}
	public static String get(String key,String defaultValue) {
	    String value = prop.getProperty(key);
	    value = value == null ? defaultValue : value;
		return value;
	}
	public static int getInt(String key,int defaultValue) {
	    String value = prop.getProperty(key);
	    if(value != null){
	        return Integer.parseInt(value);
	    }else{
	        return defaultValue;
	    }
	}
	public static boolean getBoolean(String key,Boolean defaultValue) {
	    String value = prop.getProperty(key);
	    if(StringUtils.isNotBlank(value) && value.equalsIgnoreCase("true")){
	        return true;
	    }
	    if(StringUtils.isNotBlank(value) && value.equalsIgnoreCase("false")){
	        return false;
	    }
	    return defaultValue == null ? false : defaultValue;
	}
	public static String getRelativePath(String key){
		String path = get(key);
		return sc.getRealPath(path);  
	}
}
