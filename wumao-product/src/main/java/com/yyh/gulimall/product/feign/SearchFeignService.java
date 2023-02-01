package com.yyh.gulimall.product.feign;

import com.yyh.common.to.es.SkuEsModel;
import com.yyh.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("gulimall-search")
public interface SearchFeignService {
    @PostMapping("search/save/product")
    R productStatusUp(@RequestBody List<SkuEsModel> skuEsModels);


}
