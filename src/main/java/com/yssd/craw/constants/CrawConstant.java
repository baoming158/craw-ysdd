package com.yssd.craw.constants;

import java.util.HashMap;
import java.util.Map;

public class CrawConstant {

	public static Map<String,Object> item_type = new HashMap<String,Object>();
	public static final String item_type_settle_list_keys="结算列表";
	public static final String item_type_achive_keys = "业绩公布";
	public static final String item_type_project_keys = "项目信息";
	static {
		item_type.put(item_type_settle_list_keys, "settlement_info");
		item_type.put(item_type_achive_keys, "achieve_report");
		item_type.put(item_type_project_keys, "project_info");
	}
}
