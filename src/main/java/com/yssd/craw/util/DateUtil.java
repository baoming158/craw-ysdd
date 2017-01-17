package com.yssd.craw.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class DateUtil {

    public static final String  format = "yyyy-MM-dd HH:mm:ss";
    public static final String  format_yymmdd = "yyyy-MM-dd";
    public static final  SimpleDateFormat sdf_yymmdd = new SimpleDateFormat("yyyy-MM-dd");
    public static String formatDateToString(Date date, String pattern) {
        if (null == date) return "";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(date);
    }
    
    
    public static Date parseStringToDate(String src, String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        if(src == null){
        	return null;
        }
        try {
            return sdf.parse(src);
        } catch (ParseException e) {
            return null;
        }
    }
    public static Date parseStringToDate(String src ) {
    	return parseStringToDate(src,format);
    }
    /**
	 * 取得当前日期所在周的最后一天
	 * 
	 * @param date
	 * @return
	 */
	public static Date getLastDayOfWeek(Date date) {
		Calendar c = new GregorianCalendar();
		c.setFirstDayOfWeek(Calendar.MONDAY);
		c.setTime(date);
		c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek() + 6); // Sunday
		return c.getTime();
	}
	public static Date getFirstDayOfMonth(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.MONTH, -1);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR, 0);
		calendar.set(Calendar.MINUTE,0);
		calendar.set(Calendar.SECOND,0);
		return calendar.getTime();
	}
	public static Date getLastDayOfMonth(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		calendar.add(Calendar.DATE, -1);
		return calendar.getTime();
	}
	/**
	 * 获取本月的天数
	 * @param date
	 * @return
	 */
	public static int getDayOfMonth(Date date){
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.MONTH, 1);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		calendar.add(Calendar.DATE, -1);
		return calendar.get(Calendar.DAY_OF_MONTH);
	}
    
    public static int timeCompare(Date t1, Date t2) {
		Calendar c1 = Calendar.getInstance();
		Calendar c2 = Calendar.getInstance();
		try {
			c1.setTime(t1);
			c2.setTime(t2);
		} catch (Exception e) {
			e.printStackTrace();
		}
		int result = c1.compareTo(c2); // 返回值：1:c1>c2 0:c1=c2 -1:c1<c2
		return result;
	}
    /**
     * day 天前的日期  负数为之后的日期
     * @param date
     * @param day
     * @return
     */
    public static Date getDayBefore(Date date,int day) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.DAY_OF_MONTH, -day);
		date = calendar.getTime();
		return date;
	}
    public static String getDayBeforeFormat(Date date,int day) {
    	Calendar calendar = Calendar.getInstance();
    	calendar.setTime(date);
    	calendar.add(Calendar.DAY_OF_MONTH, -day);
    	date = calendar.getTime();
    	return formatDateToString(date,"yyyy-MM-dd");
    }
	/**
	 * 根据日期和天数参数，返回增加后的日期
	 * @param trigger_time 首次触发日期
	 * @param days 要增加的天数
	 * @return long 
	 */
	@SuppressWarnings("unused")
	private static long addDate(final Date trigger_time,final Integer days){
		Calendar cal = Calendar.getInstance();
		cal.setTime(trigger_time);
		cal.add(Calendar.DATE, days);
		return cal.getTimeInMillis();
	}
	
	/**
	 * 字符串转java.sql.Date
	 * @param date
	 * @param format
	 * @return
	 */
	public static java.util.Date formatToDate(String date,String format){
		java.util.Date  sqlDate=null;
		try {
			if(date!=null && date.length()>0){
				java.util.Date  _date  =  new SimpleDateFormat(format).parse(date);     
				sqlDate  =  new java.util.Date(_date.getTime());
			}
		} catch (ParseException e) {
			e.printStackTrace();
		} 
	    return sqlDate;
	}
	/**
	 * 根据两个日期返回相隔的天数
	 * @param time1 当前日期
	 * @param time2 首次推送日期
	 * @return long 
	 */
	public static long getQuot(String time1, String time2){
		long quot = 0;
		try {
			Date date1 = sdf_yymmdd.parse(time1);
			Date date2 = sdf_yymmdd.parse(time2);
			quot = date1.getTime() - date2.getTime();
			quot = quot / 1000 / 60 / 60 / 24;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return quot;
	}
	public static void main(String[] args) {
		Date d = DateUtil.getFirstDayOfMonth(new Date());
		System.out.println(DateUtil.formatDateToString(d, format));
	}
}
