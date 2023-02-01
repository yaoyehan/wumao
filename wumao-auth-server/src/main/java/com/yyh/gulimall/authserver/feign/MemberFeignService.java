package com.yyh.gulimall.authserver.feign;

import com.yyh.common.utils.R;
import com.yyh.gulimall.authserver.vo.SocialUser;
import com.yyh.gulimall.authserver.vo.UserLoginVo;
import com.yyh.gulimall.authserver.vo.UserRegistVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("gulimall-member")
public interface MemberFeignService {
    @PostMapping("/member/member/regist")
    R regist(@RequestBody UserRegistVo vo);
    @PostMapping("/member/member/login")
    R login(@RequestBody UserLoginVo vo);
    @PostMapping("/member/member/oauth2/login")
    R oauthlogin(@RequestBody SocialUser socialUser) throws Exception;
}
