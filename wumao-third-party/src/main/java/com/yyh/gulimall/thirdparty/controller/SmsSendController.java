package com.yyh.gulimall.thirdparty.controller;

import com.yyh.common.utils.R;
import com.yyh.gulimall.thirdparty.component.SmsComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("sms")
public class SmsSendController {
    @Autowired
    SmsComponent smsComponent;
    @GetMapping("sendcode")
    public R sendCode(@RequestParam("phone")  String phone,@RequestParam("code") String code){
        smsComponent.sendSmsCode(phone,code);
        return R.ok();
    }

}
