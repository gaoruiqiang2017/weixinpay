package com.weixinpay.demo.utils;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @Author gaoruiqiang
 * @Description
 * @Date:03
 */
public class HttpUtil {

    public static String doPost(String url, String requestXml) {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse httpResponse = null;
        //创建httpClient连接对象
        httpClient = HttpClients.createDefault();
        //创建post请求连接对象
        HttpPost httpPost = new HttpPost(url);
        //创建连接请求对象,并设置连接参数
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(15000)   //连接服务区主机超时时间
                .setConnectionRequestTimeout(60000) //连接请求超时时间
                .setSocketTimeout(60000).build(); //设置读取响应数据超时时间
        //为httppost请求设置参数
        httpPost.setConfig(requestConfig);
        //将上传参数放到entity属性中
        httpPost.setEntity(new StringEntity(requestXml, "UTF-8"));
        //添加头信息
        httpPost.addHeader("Content-type", "text/xml");
        String result = "";
        try {
            //发送请求
            httpResponse = httpClient.execute(httpPost);
            //从相应对象中获取返回内容
            HttpEntity entity = httpResponse.getEntity();
            result = EntityUtils.toString(entity, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;

    }

    /**
     * 获取IP地址
     *
     * @param request
     * @return
     */
    public static String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
