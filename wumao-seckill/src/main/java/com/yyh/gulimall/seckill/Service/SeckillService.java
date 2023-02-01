package com.yyh.gulimall.seckill.Service;

import com.yyh.gulimall.seckill.to.SeckillSkuRedisTo;
import org.springframework.stereotype.Service;

import java.util.List;


public interface SeckillService {
    void uploadSeckillSkuLatest3Days();

    List<SeckillSkuRedisTo> getCurrentSeckillSkus() ;

    SeckillSkuRedisTo getSkuSeckilInfo(Long skuId);

    String kill(String killId, String key, Integer num) throws InterruptedException;
}
