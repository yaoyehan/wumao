package com.yyh.gulimall.order.feign;

import com.yyh.gulimall.order.vo.MemberAddressVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient("gulimall-member")
public interface MemberFeignService {
    @GetMapping(value = "member/memberreceiveaddress/{memberId}/address")
    List<MemberAddressVo> getAddress(@PathVariable("memberId") Long memberId);
}
