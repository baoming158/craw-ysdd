package com.yssd.craw.finance.craw;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.nutz.dao.entity.Record;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yssd.craw.constants.CrawConstant;
import com.yssd.craw.util.BigDecimalTool;
import com.yssd.craw.util.DaoUtil;
import com.yssd.craw.util.DateUtil;
import com.yssd.craw.util.HttpClientUtil;

public class StoneCashCraw implements Job{

	public static Logger log = LoggerFactory.getLogger(StoneCashCraw.class);
	
	public static AtomicLong settle_daily_new_count = new AtomicLong();//结算信息每日新增数
	
	public static AtomicLong achieve_daily_new_count = new AtomicLong();//业绩公布每日更新数
	
	public static AtomicLong project_daily_new_count = new AtomicLong();//项目每日更新
	
	public static void main(String[] args) {
		String url = "http://www.vipysdd.com/node/getNode.html?jq_random=0.25915";
		doCraw(url);
	}

	private static void doCraw(String url) {
		resetCounter();
		Map<String,String> params = new HashMap<String,String>();
		params.put("nodeType", "znxx");
		HttpClientUtil client = new HttpClientUtil();
		String string = client.doPost(url, params, "utf-8");
		JSONObject jb = JSON.parseObject(string);
		Object o = jb.get("msg");
		JSONObject rows = JSON.parseObject(o.toString());
		JSONArray array = rows.getJSONArray("rows"); 
		String recentDate = getRecentDate();
		for (int i = 0; i < array.size(); i++) {
			JSONObject job = array.getJSONObject(i); 
			String id = job.getString("id");
			String title = job.getString("title");
			if (title.contains(CrawConstant.item_type_settle_list_keys)) {
				parseSettleDetailHandler(id);
			}else if(title.contains(CrawConstant.item_type_project_keys)){
				parseProjectDetailHandler(id);
			}else if(title.contains(CrawConstant.item_type_achive_keys)){
				parseAchievesDetailHandler(id , recentDate);
			}else{
				log.info("there is nothing to do");
			}
		}
		String settle_tips = "%s 新增结算项目 %s 个";
		settle_tips = String.format(settle_tips, DateUtil.formatDateToString(new Date(), "yyyy-MM-dd"),settle_daily_new_count);
		log.info(settle_tips);
		String achives_tips = "%s 业绩公布更新%s 个";
		achives_tips = String.format(achives_tips, DateUtil.formatDateToString(new Date(), "yyyy-MM-dd"),achieve_daily_new_count);
		log.info(achives_tips);
		String project_tips = "%s 项目信息更新 %s 个";
		project_tips = String.format(project_tips, DateUtil.formatDateToString(new Date(), "yyyy-MM-dd"),project_daily_new_count);
		log.info(project_tips);
	}

	public static String getRecentDate(){
		String recentUpdateSql = "select max(update_date) max_update from achieve_report";
		Record record = DaoUtil.newInstance().queryOneBysql(recentUpdateSql);
		if(record !=null){
			return record.getString("max_update");
		}
		return null;
	}
	private static void parseAchievesDetailHandler(String id, String recentDate) {
		String url = "http://www.vipysdd.com/node/getNodeDetail.html?jq_random=0.64322";
		Document doc;
		try {
			Map<String,String> cookie =new HashMap<String,String>();
			cookie.put("JSESSIONID", "E6F54FC85B1F6EB2E29485253F24FDF6");
			cookie.put("logined", "y");
			Map<String,String> data = new HashMap<String,String>();
			data.put("id", id);
			doc = Jsoup.connect(url).cookies(cookie).data(data).timeout(100000).post();
			Elements elements=doc.select("tbody tr");
			elements.remove(0);
			elements.remove(elements.size()-2);
			String update_date =getUpdateDate(elements);
			elements.remove(elements.size()-1);
			for(Element element : elements) {
				String first = element.select("td").get(0).text();
				String [] firstspace =first.split(" ");
				String project_name = firstspace[0];
				if(checkProjectIsExits(project_name,update_date)){
					continue;
				}
				String tender_type = firstspace[1];
				tender_type = tender_type.replaceAll("【", "");
				tender_type = tender_type.replaceAll("】", "");
				String settle_date = element.select("td").get(1).text();
				String positions = element.select("td").get(2).text();
				positions=positions.replaceAll("%", "");
				String current_rate = element.select("td").get(3).text();
				current_rate=current_rate.replaceAll("%", "");
				String type = getType(tender_type);
				Record  yesterday_current_rate = getYesterDayCurrentRate(project_name,recentDate);
				String rate_increase ="";
				String positions_increase="";
				if(yesterday_current_rate != null){
					String yesterday_rate = yesterday_current_rate.getString("current_rate");
					if(StringUtils.isBlank(yesterday_rate)){
						yesterday_rate="0";
					}
					double increase = BigDecimalTool.sub(current_rate, yesterday_rate);
					rate_increase = String.valueOf(increase);
					String yesterday_positions = yesterday_current_rate.getString("positions");
					if(StringUtils.isBlank(yesterday_positions)){
						yesterday_positions="0";
					}
					double increase_positions = BigDecimalTool.sub(positions, yesterday_positions);
					positions_increase = String.valueOf(increase_positions);
				}
				String insertSql = "insert into achieve_report(project_name,settle_date,update_date,current_rate,positions,tender_type,type,rate_increase,positions_increase) VALUES ('%s','%s','%s','%s','%s','%s','%s','%s','%s')";
				String sql = String.format(insertSql, project_name,settle_date,update_date,current_rate,positions,tender_type,type,rate_increase,positions_increase);
				DaoUtil.newInstance().executeSql(sql);
				achieve_daily_new_count.incrementAndGet();
			}
		} catch (Exception e) {
			log.error("解析明细出错",e);
		}
	}

	private static boolean checkProjectIsExits(String project_name, String update_date) {
		String sql = "select * from achieve_report where project_name='%s' and update_date='%s'";
		sql = String.format(sql, project_name,update_date);
		Record record = DaoUtil.newInstance().queryOneBysql(sql);
		if(record !=null){
			return true;
		}
		return false;
	}

	private static Record getYesterDayCurrentRate(String project_name, String update_date) {
		String sql = "select * from achieve_report where project_name ='%s' and update_date='%s'";
		sql = String.format(sql, project_name,update_date);
		return DaoUtil.newInstance().queryOneBysql(sql);
	}

	private static String getUpdateDate(Elements elements) {
		String theDate = getEveryDayBottom(elements.get(elements.size()-1));
		theDate=theDate.replaceAll("更新日期：", "");
		Date update_date = DateUtil.formatToDate(theDate, "yyyy年MM月dd日");
		String updateDate = DateUtil.formatDateToString(update_date, "yyyy-MM-dd");
		return updateDate;
	}

	private static String getType(String tender_type) {
		String type ="";
		type = tender_type.replaceAll("T\\+", "");
		type = type.replaceAll("\\*3", "");
		return type;
	}

	private static String getEveryDayBottom(Element element) {
		return element.select("td span").text();
	}


	private static void parseProjectDetailHandler(String id) {
		String url = "http://www.vipysdd.com/node/getNodeDetail.html?jq_random=0.64322";
		Document doc;
		try {
			Map<String,String> cookie =new HashMap<String,String>();
			cookie.put("JSESSIONID", "E6F54FC85B1F6EB2E29485253F24FDF6");
			cookie.put("logined", "y");
			Map<String,String> data = new HashMap<String,String>();
			data.put("id", id);
			doc = Jsoup.connect(url).cookies(cookie).data(data).timeout(100000).post();
			Elements elements=doc.select("tbody tr");
			elements.remove(0);
			elements.remove(elements.size()-1);
			for(Element element : elements) {
				String trader_date = element.select("td").get(0).text();
				String project_name = element.select("td").get(1).text();
				if(checkProjectIsExists(trader_date,project_name)){
					continue;
				}
				String appointment_money = element.select("td").get(2).text();
				String venture_money = element.select("td").get(3).text();
				String trader_money = element.select("td").get(4).text();
				String partner_count = element.select("td").get(5).text();
				String status = element.select("td").get(6).text();
				String insertSql = "insert into project_info(trader_date,project_name,appointment_money,venture_money,trader_money,partner_count,status) VALUES ('%s','%s','%s','%s','%s',%s,'%s')";
				String sql = String.format(insertSql, trader_date,project_name,appointment_money,venture_money,trader_money,partner_count,status);
				DaoUtil.newInstance().executeSql(sql);
				project_daily_new_count.incrementAndGet();
			}
		} catch (Exception e) {
			log.error("解析明细出错",e);
		}
		
	}

	private static boolean checkProjectIsExists(String trader_date, String project_name) {
		String sql = "select * from project_info where trader_date='%s' and project_name ='%s'";
		sql = String.format(sql, trader_date,project_name);
		Record record = DaoUtil.newInstance().queryOneBysql(sql);
		if(record !=null){
			return true;
		}
		return false;
	}

	private static void parseSettleDetailHandler(String id) {
		String url = "http://www.vipysdd.com/node/getNodeDetail.html?jq_random=0.64322";
		Document doc;
		try {
			Map<String,String> cookie =new HashMap<String,String>();
			cookie.put("JSESSIONID", "E6F54FC85B1F6EB2E29485253F24FDF6");
			cookie.put("logined", "y");
			Map<String,String> data = new HashMap<String,String>();
			data.put("id", id);
			doc = Jsoup.connect(url).cookies(cookie).data(data).timeout(100000).post();
			Elements elements=doc.select("tbody tr");
			elements.remove(0);
			elements.remove(elements.size()-1);
			for(Element element : elements) {
				String settle_date = element.select("td").get(0).text();
				String project_name = element.select("td").get(1).text();
				if(checkProjectIsExists(project_name)){
					continue;
				}
				String total_money = element.select("td").get(2).text();
				String user_return_rate = element.select("td").get(3).text();
				String trader_return_rate = element.select("td").get(4).text();
				String ransom = element.select("td").get(5).text();
				String insertSql = "insert into settlement_info(project_name,settle_date,total_money,user_return_rate,trader_return_rate,ransom) VALUES ('%s','%s',%s,'%s','%s','%s')";
				String sql = String.format(insertSql, project_name,settle_date,total_money,user_return_rate,trader_return_rate,ransom);
				DaoUtil.newInstance().executeSql(sql);
				settle_daily_new_count.incrementAndGet();
			}
		} catch (Exception e) {
			log.error("解析明细出错",e);
		}
	}

	private static boolean checkProjectIsExists(String project_name) {
		String sql = "select * from settlement_info where project_name='"+project_name+"'";
		Record record = DaoUtil.newInstance().queryOneBysql(sql);
		if(record !=null){
			return true;
		}
		return false;
	}	
	
	private static void resetCounter(){
		settle_daily_new_count.set(0);
		achieve_daily_new_count.set(0);
		project_daily_new_count.set(0);
    }

	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		log.info("start craw ysdd info");
		String url = "http://www.vipysdd.com/node/getNode.html?jq_random=0.25915";
		try {
			doCraw(url);
		} catch (Exception e) {
			log.error("craw ysdd info error",e);
		}
		log.info("end craw ysdd info");
		
	}

}
