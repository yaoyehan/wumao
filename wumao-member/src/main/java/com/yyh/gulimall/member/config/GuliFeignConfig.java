package com.yyh.gulimall.member.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Configuration
public class GuliFeignConfig {
    @Bean("requestInterceptor")
    public RequestInterceptor requestInterceptor(){
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate requestTemplate) {
                //RequestContextHolder是刚进来的这个请求

                ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if(requestAttributes!=null){
                    HttpServletRequest request = requestAttributes.getRequest();
                    //同步请求头参数，cookie
                    String cookie = request.getHeader("Cookie");
                    //给新请求同步老请求的cookie
                    requestTemplate.header("Cookie", cookie);
                }


            }
        };
    }
}
