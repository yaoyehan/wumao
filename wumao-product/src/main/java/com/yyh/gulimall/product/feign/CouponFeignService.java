package com.yyh.gulimall.product.feign;

import com.yyh.common.to.SkuReductionTo;
import com.yyh.common.to.SpuBoundTo;
import com.yyh.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient("gulimall-coupon")
public interface CouponFeignService {
    @PostMapping("coupon/spubounds/save")
    R saveSpuBounds( @RequestBody SpuBoundTo spuBoundTo);
    @PostMapping("coupon/skufullreduction/saveinfo")
    R saveSkuReduction(@RequestBody SkuReductionTo skuReductionTo);
}
