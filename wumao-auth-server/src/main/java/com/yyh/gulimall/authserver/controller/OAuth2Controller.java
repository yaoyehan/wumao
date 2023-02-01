package com.yyh.gulimall.authserver.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.yyh.common.utils.HttpUtils;
import com.yyh.common.utils.R;
import com.yyh.common.vo.MemberRespVo;
import com.yyh.gulimall.authserver.feign.MemberFeignService;
import com.yyh.gulimall.authserver.vo.SocialUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Controller
@Slf4j
public class OAuth2Controller {
    @Autowired
    MemberFeignService memberFeignService;
    @GetMapping("/oauth2.0/weibo/success")
    public String weibo(@RequestParam("code") String code, HttpSession session) throws Exception {
        Map<String, String> headers = new HashMap<>();
        Map<String, String> querys = new HashMap<>();
        Map<String, String> map = new HashMap<>();

        map.put("grant_type","authorization_code");
        map.put("code",code);
        map.put("client_id","14771ef7be05e2fb1325de3574943432062a6118d7fb62a2bcf943b25ae62908");
        map.put("redirect_uri","http://auth.gulimall.com/oauth2.0/weibo/success");
        map.put("client_secret","e48490067bd76e76bc21dfe0def59a0fca733bd7bd65093dce5df9c10b51e138");
        //1.更具code换取accessToken
        HttpResponse response=HttpUtils.doPost("https://gitee.com","/oauth/token","post",headers,querys,map);
        //2.处理
        if(response.getStatusLine().getStatusCode()==200){
            //获取到了accessToken
            String json=EntityUtils.toString(response.getEntity());
            SocialUser socialUser = JSON.parseObject(json, SocialUser.class);
            //当前用户如果是第一次进入网站，为当前用户自动注册进来（为当前用户自动生成一个会员信息账号）
            R oauthlogin = memberFeignService.oauthlogin(socialUser);
            if(oauthlogin.getCode()==0){
                MemberRespVo data = oauthlogin.getData("data", new TypeReference<MemberRespVo>() {
                });
                log.info("登陆成功"+data.toString());
                session.setAttribute("loginUser",data);
                return "redirect:http://gulimall.com";
            }else {
                return "redirect:http://auth.gulimall.com/login.html";
            }
        }else {
            return "redirect:http://auth.gulimall.com/login.html";
        }

    }
}
