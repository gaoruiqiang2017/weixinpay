package com.weixinpay.demo.weixinAppletPay;

import com.github.wxpay.sdk.WXPayUtil;
import com.weixinpay.demo.utils.HttpUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
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
@RequestMapping("/weixinAppletpay")
public class WeixinAppletPay {


    @Value("${wxAppletAppId}")
    private String wxAppletAppId;  //小程序账号id

    @Value("${mchid}")
    private String mchId;  //商户号

    @Value("${weixinKey}")
    private String weixinKey;  //密匙

    @Value("${wxAppletAppSercet}")
    private String wxAppletAppSercet; //	小程序的appsecret

    @Value("https://api.weixin.qq.com/sns/jscode2session")
    private String wxCode2SeesionUrl; //	小程序的appsecret

    @Value("https://api.mch.weixin.qq.com/pay/unifiedorder")
    private String unifiedorderUrl; //统一下单接口

    @Value("https://127.0.0.1:8080/weixinAppletpay/AppletPay")
    private String redirectUrl;  //授权后重定向的回调链接地址， 请使用 urlEncode 对链接进行处理


    /**
     登录+支付 code
     流程大概分为几步：
     1）登录，获取code（一个code只能用一次）
     2）通过code获取openid（通过请求服务器，由服务器请求微信获取并返回小程序）。微信登录+获取openid接口
     3）小程序请求服务器进行预下单，上传商品详情、金额、openid。
     4）服务器端接收请求，根据请求订单数据、生成第三方订单号，调用微信的统一下单接口,返回prepay_id。
     5）服务器收到预下单信息后，签名并组装支付数据，返回给小程序。
     6）小程序前端发起支付，并支付完成
     7）服务器收到回调。
     */


    /**
     * //第一步：登录，获取code（一个code只能用一次）,通过code获取openid
     * 获取code参考:https://developers.weixin.qq.com/miniprogram/dev/api/wx.login.html
     *
     * @param code
     * @param httpServletRequest
     * @param httpServletResponse
     */
    public String getOpenid(HttpServletRequest httpServletRequest, HttpServletResponse
            httpServletResponse, String code, Writer writer) {
        String openid = "";
        try {
            //封装参数
            HashMap<String, String> data = new HashMap<>();
            data.put("appid", wxAppletAppId);
            data.put("secret", wxAppletAppSercet);
            data.put("js_code", code);
            data.put("grant_type", "authorization_code");
            //将类型为map的参数转换为xml
            String requestXml = WXPayUtil.mapToXml(data);
            //发送参数,调用微信统一下单接口,返回xml
            String responseXml = HttpUtil.doPost(wxCode2SeesionUrl, requestXml);
            Map<String, String> map = WXPayUtil.xmlToMap(responseXml);
            if (null != map.get("errmsg")) {
                return "操作失败";
            } else {
                //成功可以先把openid缓存起来,可以设置自己的小程序登录有效时间,也可以直接下订单
                String session_key = map.get("session_key");
                openid = map.get("openid");
                String unionid = map.get("unionid");
            }
        } catch (Exception e) {
            return "异常";
        }
        return openid;
    }

    /**
     * 支付
     * @param httpServletRequest
     * @param httpServletResponse
     * @param openid
     * @param orderId   订单id
     * @param writer
     * @param model
     * @return
     */
    public String appletPay(HttpServletRequest httpServletRequest, HttpServletResponse
            httpServletResponse, String openid, String orderId, Writer writer, Model
                                    model) {
        String mapStr = "";
        try {
            //获得openid调用微信统一下单接口
            HashMap<String, String> dataMap = new HashMap<>();
            dataMap.put("appid", wxAppletAppId); //公众账号ID
            dataMap.put("mch_id", mchId); //商户号
            dataMap.put("nonce_str", WXPayUtil.generateNonceStr()); //随机字符串，长度要求在32位以内。
            dataMap.put("body", "手机"); //商品描述,通过订单id获得
            dataMap.put("out_trade_no", orderId); //商品订单号,用户下订单后台生成
            dataMap.put("total_fee", "1"); //商品金,通过订单id获得
            dataMap.put("spbill_create_ip", HttpUtil.getIpAddress(httpServletRequest)); //客户端ip
            //通知地址(需要是外网可以访问的)
            dataMap.put("notify_url", "https://127.0.0.1:8080/weixinAppletpay/notifyUrl");
            dataMap.put("trade_type", "JSAPI"); //交易类型
            dataMap.put("openid", openid); //商户号
            //生成签名
            String signature = WXPayUtil.generateSignature(dataMap, weixinKey);
            dataMap.put("sign", signature);//签名
            //将类型为map的参数转换为xml
            String requestXml = WXPayUtil.mapToXml(dataMap);
            //发送参数,调用微信统一下单接口,返回xml
            String responseXml = HttpUtil.doPost(unifiedorderUrl, requestXml);
            Map<String, String> responseMap = WXPayUtil.xmlToMap(responseXml);
            if ("FAIL".equals(responseMap.get("return_code"))) {
                mapStr = responseMap.get("return_msg");
                writer.write(mapStr);
                return "";
            }
            if ("FAIL".equals(responseMap.get("result_code"))) {
                mapStr = responseMap.get("err_code_des");
                writer.write(mapStr);
                return "";
            }
            if ("".equals(responseMap.get("prepay_id")) || responseMap.get("prepay_id") ==
                    null) {
                writer.write("prepay_id 为空");
                return "";
            }
            //成功之后,提取prepay_id,重点就是这个
            HashMap<String, String> params = new HashMap<>();
            params.put("appId", wxAppletAppId);
            params.put("nonceStr", WXPayUtil.generateNonceStr());
            params.put("package", responseMap.get("prepay_id"));
            params.put("signType", "MD5");
            params.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
            //重新签名
            String paySign = WXPayUtil.generateSignature(params, weixinKey);
            params.put("paySign", paySign);
            //传给前端页面
            //在微信浏览器里面打开H5网页中执行JS调起支付。接口输入输出数据格式为JSON。
            mapStr = params.toString();
            //前端接受参数调用wx.requestPayment(OBJECT)发起微信支付
            //返回requestPayment:ok,支付成功
        } catch (Exception e) {

        }
        return mapStr;
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

        //判断接受到的result_code是不是SUCCESS,如果是,则返回成功,具体业务具体分析,修改订单状态

        // 通知微信.异步确认成功.必写.不然会一直通知后台.
        String resXml = "<xml>" + "<return_code><![CDATA[SUCCESS]]></return_code>" +
                "<return_msg><![CDATA[OK]]></return_msg>" + "</xml> ";
        return resXml; //或者 return "success";
    }
}
