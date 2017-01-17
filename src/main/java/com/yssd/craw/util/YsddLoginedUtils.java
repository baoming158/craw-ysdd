package com.yssd.craw.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.http.NameValuePair;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * @author windom
 *
 */
public class YsddLoginedUtils {
    private static final Logger log = LoggerFactory.getLogger(YsddLoginedUtils.class);

    private final HttpUtil ysddUtil;
    private String uname;


    public YsddLoginedUtils(String user, String passwd) throws Exception {
        ysddUtil = new HttpUtil();
        ysddUtil.setIgnoreCookies(false);
        ysddUtil.setSocketTimeOut(60000);
        ysddUtil.setConnectionTimeOut(60000);
        uname = user;
        Cookie cook1 = new BasicClientCookie("JSESSIONID", "1BBD822E28031C1ACC2BD47413975E5E");
        Cookie cook2 = new BasicClientCookie("logined", "y");
        ysddUtil.addCookies(cook1);
        ysddUtil.addCookies(cook2);
        ysddUtil.init();

        List<NameValuePair> params = new ArrayList<NameValuePair>();

//        params.add(new BasicNameValuePair("userId", user));
//        params.add(new BasicNameValuePair("password", passwd));
//        params.add(new BasicNameValuePair("valiCode", ""));
//
//        Map<String, String> additionalHeaders = new HashMap<String,String>();
//        additionalHeaders.put("Referer", "http://www.vipysdd.com/common/login.html");
//        String loginStr = ysddUtil.postHtml("http://www.vipysdd.com/common/login.html", params, "UTF-8",
//                additionalHeaders);
//
//        JSONObject jo = JSON.parseObject(loginStr);
//        String msg = jo.getString("msg");
//        if ("success".equals(msg)) {
//            log.debug("账号{}登录成功", user);
//        } else {
//            log.debug("账号{}登录失败", user);
//            throw new Exception("陨石地带账号[" + user + "]无法登录:" + msg);
//        }
    }



}
