package com.yyh.gulimall.product.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class MyRedissionConfig {
    /**
     * 所有对redisson的使用都是通过RedissonClient对象
     * @return
     * @throws IOException
     */
    @Bean(destroyMethod="shutdown")
    RedissonClient redisson() throws IOException {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.0.143:6379");
        return Redisson.create(config);
    }
}
