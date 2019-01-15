package com.weixinpay.demo.weixinH5pay;

import com.github.wxpay.sdk.WXPayUtil;
import com.weixinpay.demo.utils.HttpUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author gaoruiqiang
 * @Description
 * @Date:03
 */
@RestController
@RequestMapping("/weixinH5pay")
public class WeixinH5pay {


    @Value("${appid}")
    private String appid;  //公众账号id

    @Value("${mchid}")
    private String mchId;  //商户号

    @Value("${weixinKey}")
    private String weixinKey;  //密匙

    @Value("${unifiedorderUrl}")
    private String unifiedorderUrl; //统一下单接口

    /**
     * @param httpServletRequest
     * @param httpServletResponse
     * @param orderNo             订单号(统一下单接口前自己要又订单)
     * @param money               金额
     * @param body                商品内容
     */
    @RequestMapping("/h5pay")
    public void h5pay(HttpServletRequest httpServletRequest, HttpServletResponse
            httpServletResponse, String orderNo, String money, String body, Writer writer)
            throws Exception {
        String mapStr = "";
        try {
            HashMap<String, String> dataMap = new HashMap<>();
            dataMap.put("appid", appid); //公众账号ID
            dataMap.put("mch_id", mchId); //商户号
            dataMap.put("nonce_str", WXPayUtil.generateNonceStr()); //随机字符串，长度要求在32位以内。
            dataMap.put("body", body); //商品描述
            dataMap.put("out_trade_no", orderNo); //商品订单号
            dataMap.put("total_fee", money); //商品金
            dataMap.put("spbill_create_ip", HttpUtil.getIpAddress(httpServletRequest)); //客户端ip
            dataMap.put("notify_url", "www.baidu.com"); //通知地址(假设是百度)
            dataMap.put("trade_type", "MWEB"); //交易类型
            //   dataMap.put("scene_info", "{\"h5_info\": {\"type\":\"Wap\",\"wap_url\":
            // \"http://www" +
            //         ".baidu.com\",\"wap_name\": \"学易资源分享平台\"}}"); //场景信息(其实不写能用)
            //生成签名
            String signature = WXPayUtil.generateSignature(dataMap, weixinKey);
            dataMap.put("sign", signature);//签名
            //将类型为map的参数转换为xml
            String requestXml = WXPayUtil.mapToXml(dataMap);
            //发送参数,调用微信统一下单接口,返回xml
            String responseXml = HttpUtil.doPost(unifiedorderUrl, requestXml);
            System.out.print(responseXml);
            Map<String, String> map = WXPayUtil.xmlToMap(responseXml);
            if ("FAIL".equals(map.get("return_code"))) {
                mapStr = map.get("return_msg");
                writer.write(mapStr);
                return;
            }
            if ("FAIL".equals(map.get("result_code"))) {
                mapStr = map.get("err_code_des");
                writer.write(mapStr);
                return;
            }
            if (map.get("mweb_url") == null || "".equals(map.get("mweb_url"))) {
                mapStr = "mweb_url为null";
                writer.write(mapStr);
                return;
            }
            //成功返回了mweb_url,拼接支付成功后微信跳转自定义页面
            //确认支付过后跳的地址redirectUrl,需要经过urlencode处理(可以不写,会跳转默认原吊起微信的页面
            // 写了之后前端接收订单id后再传给后端,处理订单状态)

            //String redirectUrl = "http://www.xxxx.com/xxxxx/my_waRecord.html?orderNo=" + orderNo;
            //redirectUrl = URLEncoder.encode(redirectUrl, "utf-8");
            String url = map.get("mweb_url");//+ "&redirect_url=" + redirectUrl;
            //自动跳转微信
            StringBuilder urlHtml = new StringBuilder();
            urlHtml.append("<form id=\"weixinPay\" name=\"weixinPay\" action=\"" + url + "\" " +
                    "method=\"" + "post" + "\">");
            urlHtml.append("<input type=\"submit\" value=\"" + "payButton" + "\" " +
                    "style=\"display:none;\"></form>");
            urlHtml.append("<script>document.forms['weixinPay'].submit();</script>");
            httpServletResponse.setContentType("text/html;charset=utf-8");
            httpServletResponse.getWriter().write(urlHtml.toString());
            httpServletResponse.getWriter().flush();
        } catch (Exception e) {
            mapStr = "异常";
        }
        writer.write(mapStr);
    }

    /**
     * 异步回调(必须有,得发布到外网)
     *
     * @param unifiedorderUrl
     * @param requestXml
     * @return
     */
    @RequestMapping("/notifyUrl")
    public String notifyUrl(String unifiedorderUrl, String requestXml) {
        System.out.print("进入支付h5回调=====================");
        //如果没有加redirectUrl,就这这个接口处理订单信息
        //判断接受到的result_code是不是SUCCESS,如果是,则返回成功,具体业务具体分析
        // 通知微信.异步确认成功.必写.不然会一直通知后台.八次之后就认为交易失败了
        String resXml = "<xml>" + "<return_code><![CDATA[SUCCESS]]></return_code>" +
                "<return_msg><![CDATA[OK]]></return_msg>" + "</xml> ";
        return resXml; //或者 return "success";
    }
}
