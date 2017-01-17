package com.yssd.craw.launch;

import org.nutz.log.Logs;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yssd.craw.finance.craw.StoneCashCraw;
import com.yssd.craw.util.ResourceHelper;


public class Launch {
	protected static Logger log = LoggerFactory.getLogger(Launch.class);

	public static void run(Class<? extends Job> clz,String cron) throws Exception {
		SchedulerFactory sf = new StdSchedulerFactory();
		Scheduler sched = sf.getScheduler();
		JobDetail job = JobBuilder.newJob(clz).withIdentity(clz.getName(), "group-weibo").build();
		Trigger trigger = TriggerBuilder.newTrigger().withIdentity(cron, "group-weibo")
				.withSchedule(CronScheduleBuilder.cronSchedule(cron)).build();
		sched.scheduleJob(job, trigger);
		sched.start();
	}
	/**
	 * Seconds Minutes Hours DayofMonth Month DayofWeek Year或
	 * Seconds Minutes Hours DayofMonth Month DayofWeek 
	 */
	
	public static void main(String[] args) throws Exception {
		Logs.setAdapter(Logs.NOP_ADAPTER);
		log.info("程序启动");
		try {
			run(StoneCashCraw.class , ResourceHelper.get("StoneCashCraw"));//每天00:02时执行 ：全天（00:00-24:00）卫视频道排行榜
		} catch (Throwable e) {
			log.error("程序异常",e);
		}
	}
}
