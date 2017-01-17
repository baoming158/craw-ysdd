/**
 * Copyright (C) 2011-2021 liepin Inc.All Rights Reserved.
 * 
 * FileName:MyHttpRequestRetryHandler.java
 *
 * Description：简要描述本文件的内容
 *
 * History：
 * 版本号           作者                  日期               简要介绍相关操作
 *  1.0  windom  2016-5-12
 *
 */
package com.yssd.craw.util;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.HttpContext;

/**
 * @author windom
 *
 */
public class MyHttpRequestRetryHandler implements HttpRequestRetryHandler {

    /* (non-Javadoc)
     * @see org.apache.http.client.HttpRequestRetryHandler#retryRequest(java.io.IOException, int, org.apache.http.protocol.HttpContext)
     */
    public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
        // executionCount ==1 表示第一次提交请求失败后,不是重试的次数
        if (executionCount >= 2) {
            return false;
        }

        // ConnectTimeoutException,SocketTimeoutException 等等
        if (exception instanceof InterruptedIOException) {
            return true;
        }
        if (exception instanceof UnknownHostException) {
            return false;
        }

        if (exception instanceof SSLException) {
            return false;
        }
        HttpClientContext clientContext = HttpClientContext.adapt(context);
        HttpRequest request = clientContext.getRequest();
        boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
        if (idempotent) {
            // Retry if the request is considered idempotent
            return true;
        }
        return false;
    }

}
