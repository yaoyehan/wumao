package com.yyh.gulimall.product.feign.fallback;


import com.yyh.common.exception.BizCodeEnume;
import com.yyh.common.utils.R;
import com.yyh.gulimall.product.feign.SeckillFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author yaoxinjia
 */
@Component
@Slf4j
public class SeckillFeignServiceFallBack implements SeckillFeignService {
    @Override
    public R getSkuSeckilInfo(Long skuId) {
        log.info("熔断操作");
        return R.error(BizCodeEnume.TOO_MANY_REQUEST.getCode(),BizCodeEnume.TOO_MANY_REQUEST.getMsg());
    }
}
