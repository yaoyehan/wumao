package com.yyh.gulimall.seckill.scheduled;

import com.yyh.gulimall.seckill.Service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
/*@EnableScheduling*/
public class SeckillSkuScheduled {
    @Autowired
    SeckillService seckillService;
    @Autowired
    RedissonClient redissonClient;
    private final String upload_lock="seckill:upload:lock";
    @Scheduled(cron = "*/3 * * * * ?")
    public void uploadSeckillSkuLatest3Days(){

        RLock lock = redissonClient.getLock(upload_lock);
        lock.lock(10, TimeUnit.SECONDS);
        try {
            seckillService.uploadSeckillSkuLatest3Days();
        }finally {
            lock.unlock();
        }
    }



}
