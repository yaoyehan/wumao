package com.yyh.gulimall.authserver.feign;

import com.yyh.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("gulimall-third-party")
public interface ThirdPartFeignService {
    @GetMapping("sms/sendcode")
    public R sendCode(@RequestParam("phone")  String phone, @RequestParam("code") String code);


    }
