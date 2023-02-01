package com.yyh.gulimall.order.feign;

import com.yyh.common.to.es.SkuHasStockVo;
import com.yyh.common.utils.R;
import com.yyh.gulimall.order.vo.WareSkuLockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("gulimall-ware")
public interface WmsFeignService {
    @PostMapping("ware/waresku/hasstock")
    R getSkuHasStock(@RequestBody List<Long> skuIds);
    @GetMapping("ware/wareinfo/fare")
    R getFare(@RequestParam("addrId") Long addrId);
    @PostMapping("ware/waresku/lock/order")
    R orderLockStock(@RequestBody WareSkuLockVo wareSkuLockVo);
}
