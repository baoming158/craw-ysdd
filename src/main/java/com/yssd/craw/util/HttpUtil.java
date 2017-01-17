package com.yssd.craw.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultServiceUnavailableRetryStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtil implements Serializable {

    private static final long serialVersionUID = -4974412413816177372L;

    private static final Logger log = LoggerFactory.getLogger(HttpUtil.class);

    private static final HttpRequestRetryHandler httpRequestRetryHandler = new MyHttpRequestRetryHandler();

    private int connectionTimeOut = 5000;

    private int socketTimeOut = 10000;
    // 是整个池子的大小
    private int maxTotal = 20;
    // 根据连接到的主机对MaxTotal的一个细分
    private int maxPerRoute = 3;

    private String userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.137 Safari/537.36";

    private boolean ignoreCookies = true;

    private boolean redirectsEnabled = true;

    private boolean autoDetectorCode = false;

    private List<Header> headers = null;

    private CloseableHttpClient closeableHttpClient = null;

    private HttpClientContext context = null;
    
    private HttpHost proxy = null;

    public HttpUtil() {
        defaultSet();
    }

    public HttpUtil(boolean autoDetectorCode) {
        this.autoDetectorCode = autoDetectorCode;
        defaultSet();
    }

    /**
     * 在使用post和get方法前必须调用这个方法初始化
     **/
    public void init() throws Exception {
        this.proxy = null;
        try {
            closeableHttpClient = newHttpClient(null);
        } catch (Exception e) {
            log.error("HttpClient初始化失败", e);
            throw new Exception("HttpClient初始化失败");
        }
    }

    /**
     * 使用代理ip 在使用post和get方法前必须调用这个方法初始化
     **/
    public void init(HttpHost proxy) throws Exception {
        this.proxy = proxy;
        try {
            closeableHttpClient = newHttpClient(proxy);
        } catch (Exception e) {
            log.error("HttpClient初始化失败", e);
            throw new Exception("HttpClient初始化失败");
        }
    }

    private void defaultSet() {
        headers = new ArrayList<Header>();
        headers.add(new BasicHeader("Cache-Control", "max-age=0"));
        headers.add(new BasicHeader("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"));
        headers.add(new BasicHeader("DNT", "1"));
        headers.add(new BasicHeader("Accept-Language", "zh-CN,zh;q=0.8"));

        initContext(new BasicCookieStore());
    }

    private void initContext(CookieStore cs) {
        context = HttpClientContext.create();
        context.setCookieStore(cs);
        context.setCredentialsProvider(new BasicCredentialsProvider());
    }

    public CloseableHttpClient newHttpClient(HttpHost proxy) {
        PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();

        poolingHttpClientConnectionManager.setMaxTotal(maxTotal);
        poolingHttpClientConnectionManager.setDefaultMaxPerRoute(maxPerRoute);

        poolingHttpClientConnectionManager.setDefaultConnectionConfig(ConnectionConfig.custom().setCharset(
                Charset.defaultCharset()).build());
        poolingHttpClientConnectionManager.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(socketTimeOut)
                .build());

        RequestConfig config = RequestConfig.custom().setProxy(proxy).setCookieSpec(
                ignoreCookies ? CookieSpecs.IGNORE_COOKIES : CookieSpecs.DEFAULT).setRedirectsEnabled(redirectsEnabled)
                .setSocketTimeout(socketTimeOut).setCircularRedirectsAllowed(true).setMaxRedirects(10)
                .setConnectTimeout(connectionTimeOut).build();

        return HttpClients.custom().setDefaultRequestConfig(config).setDefaultHeaders(headers).setRetryHandler(
                httpRequestRetryHandler).setServiceUnavailableRetryStrategy(
                new DefaultServiceUnavailableRetryStrategy(2, 2000)).setUserAgent(userAgent).setConnectionManager(
                poolingHttpClientConnectionManager).build();
    }

    /**
     * @描述：使用该方法后需要手工调用close()方法释放资源
     * @param httpRequestBase
     * @return
     * @throws Exception
     * @return CloseableHttpResponse
     * @exception @createTime：2016-4-5
     * @author: windom
     */
    public CloseableHttpResponse getOrPost(HttpRequestBase httpRequestBase) throws Exception {
        if (closeableHttpClient == null) {
            throw new Exception("Httpclient初始化失败或是未调用init方法");
        }

        try {
            return closeableHttpClient.execute(httpRequestBase, context);
        } catch (ConnectTimeoutException e) {
            // log.warn("连接超时[{}]", httpRequestBase.getString());
            throw new Exception("连接超时:" + httpRequestBase.getURI());
        } catch (SocketTimeoutException e) {
            // log.warn("读取超时[{}]", httpRequestBase.getString());
            throw new Exception("读取超时:" + httpRequestBase.getURI());
        } catch (HttpHostConnectException e) {
            // log.warn("无法连接服务器[{}]", httpRequestBase.getString());
            throw new Exception("无法连接服务器:" + httpRequestBase.getURI());
        } catch (Exception e) {
            // log.warn("抓取[{}]失败:[{}]", httpRequestBase.getString(),
            // e.toString());
            e.printStackTrace();
            throw new Exception("HTTP请求发生未知错误:" + e.toString());
        }
    }

    /**
     * @描述：使用该方法后需要手工调用close()方法释放资源
     * @param url
     * @param additionalHeaders
     * @return
     * @throws Exception
     * @return CloseableHttpResponse
     * @exception @createTime：2016-4-5
     * @author: windom
     */
    public CloseableHttpResponse get(String url, Map<String, String> additionalHeaders, RequestConfig proxyConfig)
            throws Exception {
        HttpGet get = new HttpGet(url);
        
        if(proxyConfig != null){
            get.setConfig(proxyConfig);
        }
        
        if (additionalHeaders != null) {
            for (Map.Entry<String, String> head : additionalHeaders.entrySet()) {
                get.addHeader(head.getKey(), head.getValue());
            }
        }

        return getOrPost(get);
    }

    /**
     * @描述：使用该方法后需要手工调用close()方法释放资源
     * @param url
     * @return
     * @throws Exception
     * @return CloseableHttpResponse
     * @exception @createTime：2016-4-5
     * @author: windom
     */
    public CloseableHttpResponse get(String url) throws Exception {
        return get(url, null, null);
    }

    public String getHtml(String url, Map<String, String> additionalHeaders) throws Exception {
        CloseableHttpResponse chr = get(url, additionalHeaders, null);
        if (chr != null) {
            return getStringFromHttpResponse(chr, url);
        } else {
            return null;
        }
    }

    public String getHtml(String url) throws Exception {
        return getHtml(url, new HashMap<String, String>());
    }

    public String getHtml(String url, final String referer) throws Exception {
        return getHtml(url, new HashMap<String, String>() {
            private static final long serialVersionUID = -5203058556900312598L;
            {
                put("Referer", referer);
            }
        });
    }

    /**
     * @描述：使用该方法后需要手工调用close()方法释放资源
     * @param url
     * @param httpEntity
     * @param additionalHeaders
     * @return
     * @throws Exception
     * @return CloseableHttpResponse
     * @exception @createTime：2016-4-5
     * @author: windom
     */
    public CloseableHttpResponse post(String url, HttpEntity httpEntity, Map<String, String> additionalHeaders,
            RequestConfig proxyConfig) throws Exception {
    	HttpGet post = new HttpGet(url);
//        HttpPost post = new HttpPost(url);
        
        if(proxyConfig != null){
            post.setConfig(proxyConfig);
        }
        
//        if (httpEntity != null) {
//            post.setEntity(httpEntity);
//        }

        if (additionalHeaders != null) {
            for (Map.Entry<String, String> head : additionalHeaders.entrySet()) {
                post.addHeader(head.getKey(), head.getValue());
            }
        }

        return getOrPost(post);
    }

    /**
     * @描述：使用该方法后需要手工调用close()方法释放资源
     * @param url
     * @param postParamList
     * @param charset
     * @param additionalHeaders
     * @return
     * @throws Exception
     * @return CloseableHttpResponse
     * @exception @createTime：2016-4-5
     * @author: windom
     */
    public CloseableHttpResponse post(String url, List<NameValuePair> postParamList, String charset,
            Map<String, String> additionalHeaders) throws Exception {
        HttpEntity httpEntity = null;
        if (postParamList != null) {
            httpEntity = new UrlEncodedFormEntity(postParamList, charset);
        }

        return post(url, httpEntity, additionalHeaders, null);
    }

    /**
     * @描述：使用该方法后需要手工调用close()方法释放资源
     * @param url
     * @param postParamList
     * @return
     * @throws Exception
     * @return CloseableHttpResponse
     * @exception @createTime：2016-4-5
     * @author: windom
     */
    public CloseableHttpResponse post(String url, List<NameValuePair> postParamList) throws Exception {
        return post(url, postParamList, "UTF-8", null);
    }

    public String postHtml(String url, HttpEntity httpEntity, Map<String, String> additionalHeaders) throws Exception {
        CloseableHttpResponse chr = post(url, httpEntity, additionalHeaders, null);
        if (chr != null) {
            return getStringFromHttpResponse(chr, url);
        } else {
            return null;
        }
    }

    public String postHtml(String url, HttpEntity httpEntity) throws Exception {
        return postHtml(url, httpEntity, null);
    }

    public String postHtml(String url, List<NameValuePair> postParamList, String charset,
            Map<String, String> additionalHeaders) throws Exception {
        HttpEntity httpEntity = null;
        if (postParamList != null) {
            httpEntity = new UrlEncodedFormEntity(postParamList, charset);
        }

        return postHtml(url, httpEntity, additionalHeaders);
    }

    public String postHtml(String url, List<NameValuePair> postParamList) throws Exception {
        return postHtml(url, postParamList, "UTF-8", null);
    }

    /**
     * 获取返回的html
     * 
     * @return
     * @throws Exception
     */
    public String getStringFromHttpResponse(CloseableHttpResponse chr, String url) throws Exception {
        try {
            byte[] bytes = EntityUtils.toByteArray(chr.getEntity());
            int returnCode = chr.getStatusLine().getStatusCode();
            if (returnCode == HttpStatus.SC_OK) {
                return new String(bytes, dealWithCode(bytes, "UTF-8"));
            } else {
//                log.warn("http返回状态码异常:{},{},{}", returnCode, url,new String(bytes, dealWithCode(bytes, "UTF-8")));
                throw new HttpStatusNotOKException(returnCode);
            }
        } catch (HttpStatusNotOKException e) {
            throw e;
        } catch (Exception e) {
//            log.error("处理http返回结果时出错:[{}], {}\n{}", url, e.getMessage(), e.toString());
        } finally {
            close(chr);
        }
        return null;
    }

    public byte[] getByteFromHttpResponse(CloseableHttpResponse chr, String url) throws Exception {
        try {
            byte[] bytes = EntityUtils.toByteArray(chr.getEntity());
            int returnCode = chr.getStatusLine().getStatusCode();
            if (returnCode == HttpStatus.SC_OK) {
                return bytes;
            } else {
                log.warn("http返回状态码异常:{},{}", returnCode, url);
                throw new HttpStatusNotOKException(returnCode);
            }
        } catch (HttpStatusNotOKException e) {
            throw e;
        } catch (Exception e) {
//            log.error("处理http返回结果时出错:[{}], {}\n{}", url, e.getMessage(), e.toString());
        } finally {
            close(chr);
        }
        return null;
    }

    public static void close(CloseableHttpResponse chr) {
        if (chr != null) {
            try {
                chr.close();
            } catch (Exception e) {
                log.warn("close response error:{}", e.toString());
            }
        }
    }

    public List<Cookie> getCookies() {
        List<Cookie> ret = new ArrayList<Cookie>();
        if (context != null) {
            return context.getCookieStore().getCookies();
        }
        return ret;
    }

    public String getCookieValue(String key) {
        if (key != null) {
            for (Cookie cookie : getCookies()) {
                if (key.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public void addCookies(Cookie cookie) {
        if (context != null) {
            context.getCookieStore().addCookie(cookie);
        }
    }

    public static String getRedirectUrlFromResponse(CloseableHttpResponse chr) {
        if (chr != null) {
            int returnCode = chr.getStatusLine().getStatusCode();
            if (returnCode == HttpStatus.SC_MOVED_TEMPORARILY || returnCode == HttpStatus.SC_MOVED_PERMANENTLY
                    || returnCode == HttpStatus.SC_SEE_OTHER || returnCode == HttpStatus.SC_TEMPORARY_REDIRECT) {
                Header locationHead = chr.getFirstHeader("location");
                if (locationHead != null) {
                    return locationHead.getValue();
                }
            }
        }
        return null;
    }

    // getters and setters

    public int getConnectionTimeOut() {
        return connectionTimeOut;
    }

    /**
     * @描述：在init方法后调用该方法时,必须再调用changeRequestConfig()方法才能生效
     * @param connectionTimeOut
     * @return void
     * @exception @createTime：2016-4-5
     * @author: windom
     */
    public void setConnectionTimeOut(int connectionTimeOut) {
        this.connectionTimeOut = connectionTimeOut;
    }

    public int getSocketTimeOut() {
        return socketTimeOut;
    }

    /**
     * @描述：在init方法后调用该方法时,必须再调用changeRequestConfig()方法才能生效
     * @param socketTimeOut
     * @return void
     * @exception @createTime：2016-4-5
     * @author: windom
     */
    public void setSocketTimeOut(int socketTimeOut) {
        this.socketTimeOut = socketTimeOut;
    }

    public int getMaxTotal() {
        return maxTotal;
    }

    public void setMaxTotal(int maxTotal) {
        this.maxTotal = maxTotal;
    }

    public int getMaxPerRoute() {
        return maxPerRoute;
    }

    public void setMaxPerRoute(int maxPerRoute) {
        this.maxPerRoute = maxPerRoute;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public boolean isIgnoreCookies() {
        return ignoreCookies;
    }

    /**
     * @描述：在init方法后调用该方法时,必须再调用changeRequestConfig()方法才能生效
     * @param ignoreCookies
     * @return void
     * @exception @createTime：2016-4-5
     * @author: windom
     */
    public void setIgnoreCookies(boolean ignoreCookies) {
        this.ignoreCookies = ignoreCookies;
    }

    public boolean isRedirectsEnabled() {
        return redirectsEnabled;
    }

    /**
     * @描述：在init方法后调用该方法时,必须再调用changeRequestConfig()方法才能生效
     * @param redirectsEnabled
     * @return void
     * @exception @createTime：2016-4-5
     * @author: windom
     */
    public void setRedirectsEnabled(boolean redirectsEnabled) {
        this.redirectsEnabled = redirectsEnabled;
    }

    public List<Header> getHeaders() {
        return headers;
    }

    public void setHeaders(List<Header> headers) {
        this.headers = headers;
    }

    public boolean isAutoDetectorCode() {
        return autoDetectorCode;
    }

    public void setAutoDetectorCode(boolean autoDetectorCode) {
        this.autoDetectorCode = autoDetectorCode;
    }

    public CloseableHttpClient getCloseableHttpClient() {
        return closeableHttpClient;
    }

    public HttpClientContext getContext() {
        return context;
    }

    public void setContext(HttpClientContext context) {
        this.context = context;
    }

    /**
     * @描述：当httpUtil调用init()方法后,对RequestConfig属性[ ignoreCookies,
     *                                            redirectsEnabled,
     *                                            socketTimeOut
     *                                            ,connectionTimeOut]再次修改后,
     *                                            调用本方法才能生效. @return void
     * @exception @createTime：2016-4-5
     * @author: windom
     */
    public void changeRequestConfig() {
        context.setRequestConfig(RequestConfig.copy(context.getRequestConfig()).setCookieSpec(
                ignoreCookies ? CookieSpecs.IGNORE_COOKIES : CookieSpecs.DEFAULT).setRedirectsEnabled(redirectsEnabled)
                .setSocketTimeout(socketTimeOut).setConnectTimeout(connectionTimeOut).build());
    }

    public void changeRequestConfig(RequestConfig requestConfig) {
        context.setRequestConfig(requestConfig);
    }

    /**
     * @描述：当httpStatus不为200时进入此方法,可以重写这个方法实现重新登录等后续处理
     * @param httpStatus
     * @param url
     * @return
     * @return String
     * @exception @createTime：2016-4-5
     * @author: windom
     */
    public String otherProcess(CloseableHttpResponse chr, int httpStatus, String url) {
        log.debug("{}的返回状态为{},不做任何处理,返回null", url, httpStatus);
        return null;
    }

    public String dealWithCode(byte[] bytes, String defaultCode) {
        if (isAutoDetectorCode() && bytes != null) {
            try {
                defaultCode = CodeDetector.getEncode(bytes);
                if ("ISO-8859-1".equalsIgnoreCase(defaultCode)) {
                    defaultCode = "GBK";
                }
                if ("big5".equalsIgnoreCase(defaultCode)) {
                    defaultCode = "UTF-8";
                }
                if (defaultCode.startsWith("windows")) {
                    defaultCode = "UTF-8";
                }
            } catch (Exception e) {
                log.warn("获取编码异常:{}", e.toString());
            }
        }
        return defaultCode;
    }

    public static void main(String[] args) throws Exception {
        HttpUtil hu = new HttpUtil();
        hu.init();
        System.out.println(hu.getHtml(
                "http://search.51job.com/jobsearch/search_result.php?fromJs=1&keyword=%E5%BA%84%E5%87%8C%E6%8E%A7%E8%82%A1&keywordtype=1&jobterm=01&curr_page=1"));
    }

    public static class HttpStatusNotOKException extends Exception {
        private static final long serialVersionUID = 5126122788838084114L;
        private int httpStatus;

        public HttpStatusNotOKException(int httpStatus) {
            super("httpStatus:" + httpStatus);
            this.httpStatus = httpStatus;
        }

        public int getHttpStatus() {
            return httpStatus;
        }
    }

    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.writeInt(connectionTimeOut);
        out.writeInt(socketTimeOut);
        out.writeInt(maxTotal);
        out.writeInt(maxPerRoute);
        out.writeUTF(userAgent);
        out.writeBoolean(ignoreCookies);
        out.writeBoolean(redirectsEnabled);
        out.writeBoolean(autoDetectorCode);
        out.writeObject(headers);
        out.writeObject(proxy);
        if (!ignoreCookies) {
            out.writeObject(context.getCookieStore());
        }
    }

    @SuppressWarnings("unchecked")
    private void readObject(final ObjectInputStream in) throws Exception {
        connectionTimeOut = in.readInt();
        socketTimeOut = in.readInt();
        maxTotal = in.readInt();
        maxPerRoute = in.readInt();
        userAgent = in.readUTF();
        ignoreCookies = in.readBoolean();
        redirectsEnabled = in.readBoolean();
        autoDetectorCode = in.readBoolean();
        headers = (ArrayList<Header>) in.readObject();
        proxy = (HttpHost) in.readObject();

        // cookie
        CookieStore cs = new BasicCookieStore();
        if (!ignoreCookies) {
            cs = (BasicCookieStore) in.readObject();
        }
        initContext(cs);

        // 根据各个属性生成closeableHttpClient
        if(proxy != null){
            init(proxy);
        }else{
            init();
        }
        
    }

}
