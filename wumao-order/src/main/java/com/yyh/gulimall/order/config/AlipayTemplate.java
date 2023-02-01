package com.yyh.gulimall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.yyh.gulimall.order.vo.PayVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author yaoxinjia
 * @email 894548575@qq.com
 */
@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    // 应用ID,您的APPID，收款账号既是您的APPID对应支付宝账号
    public String app_id="2021000119644159";

    // 商户私钥，您的PKCS8格式RSA2私钥
    public String merchant_private_key="MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCMFEgmx2pEWJO2LcMk1oP1psWOKIvqgPcyTYRKP6buk4p4FSDD1DVI1t7M9MoBo3feYjm9YdBLf1hRgdDISW7iThcIFKkehlWFiBjyDARbjOI+bDJN7bc+0Ge/XJfnJ91E5XQcJlGxP6yxulIOLOz5oBtc14tEWCo25reCT2vG1zdWbOtVzhcJok8XkEcvjwPsFyasED2A8BGEPCGe4qBSRwP/4TtY4ZQtzh7zVhIVFD414rHzgCTh9giI1dlEJKycZOwjH5sGyiohLawrl1Xz3GUYk8EBU+E96dceS7r123wwWzLTkmdidH7qaICndS0lqCCbCZAinTEEl8bzj4HdAgMBAAECggEAEpxDNbu6t5fZEBQmYAeMAqAEkS174Uz/CpTX5ZFtWTcK54dPEhVoeuPpN+uxxuPz8fy01R9gsBccIcAQf/uZmsFrN+yex2prhFEfl1DXZ15hVI/FYysY9GR11Cu2FsTqFdQyR+cjZVwjUn2Bl1aKqWOHHx+X5h2chizC5rIsjH8pk4D276VR4iuwenOqdakT08aiCJPUVZDIXQywqROxfLsUABiHagH4PUxOSkrHTEFMUYcvKjF2UDh1lItxbCePSUlWSjF7J6X2A2a4O3vi1fJ9ESP7MWIeYf8B3dj1lou0d5PQBsrjApyf74+5io78zNfbaM2muaOCrDnhKnWrlQKBgQDxNlZp3uzSpJWgtfH15ronLfTHVN2lANsuny253sR/RHndMHnuu07NJEzlADAdaitkuvn2g4DwLBwqa3z9FFMk9QDR4emMqG/Zt70/zqfsL7zgV76EirmDm7x8+Hwg139KpJmpHRFq7WkekSKrPJtBvCrwrK8caLNaspjkpZZOBwKBgQCUqrroHe63sYkmCrP1sFa+jTJIyIuCQtWlPpUpVnAVkyaKW+X51YrzCyKfwARNjDnkmVq7OHfa9N2Md1hF5cGcU+bGnvNGWHCNO7K0kURBwWEVcxf31ER2CJJSRbeCYGq/OV1KPxO0wukjAu4gJeONghu2oUKuFHapbodEQsa3+wKBgQDhXhN+93ntlTj33wci6WAkGxwI4ZH1CMfA15ixgSG2hxQVRaWnF/qYUMJHc9WApiHFC61gVZ2yyIPIXoV5L/Q/I2qQrrK3aOiYUsPPShY65p9iPJkU3UhXXrrNSJ0xUibsZr3q8+3CKe99T71EoGuvD1BPbghuUqTAqP2RI+U7IQKBgEzOpD/17vZCp+vajb06fKZ4BmmqCsGizARSf/faa5AJ9Vh4QCnCVMZak2nbZ520+9gKDuE5PFsINUo0AowiNtQQWON3DZLjcErXc+1sO8qJISJW0yQ8K56bsRUGZdfa/muLNJJ+Yiz6oj5vbh131cevV6gUSoHrmp7QttvicA5vAoGBAPEVQ7bxeNHn6p71zZF8r8rarohh/7kagw2XDHxoKhw6XXbNMklh6GVJZ0tEiBhmND7ajgD9zqeOkkYkH/yy5wCM9qW1qctSKieuk2cHcvc1Ia/0jGL1kW1AcZA7LzZBhw+Uib0FBKnCj3J4QsDHjfHSV8Iz4b2pitKNYSI6iuea";

    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    public String alipay_public_key="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArhSbEbqs3+xxLHrKFv7jgVo02zeDerM1NQI5fjGBGPkIkSiCp4ajYW9tN0kiRCzFPiIhdFN2W6uAZavM8Qg2JXT2tmdqEHNij5MVsz2eEbZvxDX6oeVV8clWTsvyzS0Nn0oIa+Q5r9zI40XGNTARea0sIMcKg6DeNKtnRnDkoOZ42yxncFAvQ0bASJDvXaeMnJ/EmaPxfS7sgsbZtfGVJHQf6fNCHnbK68jILoYC7LRHg7BcIUFJqD/iE7u0HO83P768uc8MRQEl6MEMAzmLLOXD47uvCd1wx9QDriQ6SyZT1WXvFLVyCAN5VJgS9IKYdxn2Dh27nh4t0JPnp0BvxwIDAQAB";

    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    public String notify_url="https://5m75d75549.zicp.fun/payed/notify";

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    public String return_url="http://member.gulimall.com/memberOrder.html";

    // 签名方式
    private  String sign_type="RSA2";

    // 字符编码格式
    private  String charset="utf-8";

    //订单超时时间
    private String timeout = "1m";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    public String gatewayUrl="https://openapi.alipaydev.com/gateway.do";

    public  String pay(PayVo vo) throws AlipayApiException {

        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                + "\"total_amount\":\""+ total_amount +"\","
                + "\"subject\":\""+ subject +"\","
                + "\"body\":\""+ body +"\","
                + "\"timeout_express\":\""+timeout+"\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝的响应："+result);

        return result;

    }
}
