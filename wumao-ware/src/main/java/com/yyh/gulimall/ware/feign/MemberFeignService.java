package com.yyh.gulimall.ware.feign;

import com.yyh.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("gulimall-member")
public interface MemberFeignService {
    @RequestMapping("member/memberreceiveaddress/info/{id}")
    R addrInfo(@PathVariable("id") Long id);
}
