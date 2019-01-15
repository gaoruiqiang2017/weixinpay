package com.weixinpay.demo.qrCodePay;

import com.github.wxpay.sdk.WXPayUtil;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.weixinpay.demo.utils.HttpUtil;
import com.weixinpay.demo.utils.MatrixToImageWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * @Author gaoruiqiang
 * @Description
 * @Date:03
 */
@RestController
@RequestMapping("/weixinpay")
public class Weixinpay {


    @Value("${appid}")
    private String appid;

    @Value("${mchid}")
    private String mchId;

    @Value("${weixinKey}")
    private String weixinKey;

    @Value("${unifiedorderUrl}")
    private String unifiedorderUrl;

    /**
     * @param httpServletRequest
     * @param httpServletResponse
     * @param orderNo             订单号
     * @param money               金额
     * @param body                商品内容
     */
    @RequestMapping("/pay")
    public void pay(HttpServletRequest httpServletRequest, HttpServletResponse
            httpServletResponse, String orderNo, String money, String body)
            throws Exception {
        try {
            HashMap<String, String> dataMap = new HashMap<>();
            dataMap.put("appid", appid); //公众账号ID
            dataMap.put("mch_id", mchId); //商户号
            dataMap.put("nonce_str", WXPayUtil.generateNonceStr()); //随机字符串，长度要求在32位以内。
            dataMap.put("body", body); //商品描述
            dataMap.put("out_trade_no", orderNo); //商品订单号
            dataMap.put("total_fee", money); //商品金
            dataMap.put("spbill_create_ip", InetAddress.getLocalHost().getHostAddress()); //客户端ip
            dataMap.put("notify_url", "www.baidu.com"); //通知地址(假设是百度)
            dataMap.put("trade_type", "NATIVE"); //交易类型
            dataMap.put("product_id", "1"); //trade_type=NATIVE时，此参数必传。商品ID，商户自行定义。
            //生成签名
            String signature = WXPayUtil.generateSignature(dataMap, weixinKey);
            dataMap.put("sign", signature);//签名
            //将类型为map的参数转换为xml
            String requestXml = WXPayUtil.mapToXml(dataMap);
            //发送参数,调用微信统一下单接口,返回xml
            String responseXml = HttpUtil.doPost(unifiedorderUrl, requestXml);
            Map<String, String> map = WXPayUtil.xmlToMap(responseXml);
            if (map.get("return_code").toString().equals("SUCCESS") && map.get("result_code")
                    .toString().equals("SUCCESS")) {
                String urlCode = (String) map.get("code_url"); //微信二维码短链接
                // 生成微信二维码，输出到response流中

                Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>();
                // 内容所使用编码
                hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
                BitMatrix bitMatrix = new MultiFormatWriter().encode(urlCode, BarcodeFormat
                        .QR_CODE, 300, 300, hints);
                // 生成二维码
                MatrixToImageWriter.writeToFile(bitMatrix, "gif", new File("C:/downloads/二维码文件" +
                        ".gif"));
            } else {
            }
        } catch (Exception e) {

        }
    }

    @RequestMapping("/notifyUrl")
    public String notifyUrl(String unifiedorderUrl, String requestXml) {
        System.out.print("h");
        return "回调成功";

    }

}
