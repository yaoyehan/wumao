package com.yyh.gulimall.search.feign;

import com.yyh.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("gulimall-product")
public interface ProductFeignService {

    @GetMapping("/product/attr/info/{attrId}")
    R attrInfo(@PathVariable("attrId") Long attrId);

    @GetMapping("/product/brand/infos")
    R BrandsInfo(@RequestParam("brandIds") List<Long> brandIds);

    @RequestMapping("product/category/info/{catId}")
    R CatInfo(@PathVariable("catId") Long catId);

}
