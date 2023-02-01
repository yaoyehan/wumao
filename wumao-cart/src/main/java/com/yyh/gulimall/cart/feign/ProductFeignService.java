package com.yyh.gulimall.cart.feign;

import com.yyh.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;

@FeignClient("gulimall-product")
public interface ProductFeignService {
    @RequestMapping("product/skuinfo/info/{skuId}")
    R getSkuInfo(@PathVariable("skuId") Long skuId);

    @GetMapping("product/skusaleattrvalue/stringlist/{skuId}")
    List<String> getSkuSaleAttrValues(@PathVariable("skuId") Long skuId);

    /**
     * 查询商品价格
     */
    @GetMapping("product/skuinfo/{skuId}/price")
    //@RequiresPermissions("product:spuinfo:delete")
    R getPrice (@PathVariable("skuId") Long skuId);

}
