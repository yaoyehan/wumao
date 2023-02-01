package com.yyh.gulimall.product.feign;

import com.yyh.common.to.es.SkuHasStockVo;
import com.yyh.common.utils.R;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("gulimall-ware")
public interface WareFeignService {
    @PostMapping("/ware/waresku/hasstock")
    R getSkuHasStock(@RequestBody List<Long> skuIds);
}
