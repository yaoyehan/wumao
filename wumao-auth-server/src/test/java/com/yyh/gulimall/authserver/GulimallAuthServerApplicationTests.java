package com.yyh.gulimall.authserver;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Random;
import java.util.UUID;

@SpringBootTest
class GulimallAuthServerApplicationTests {

    @Test
    void contextLoads() {

        System.out.println(UUID.randomUUID().toString().replaceAll("-", ""));
    }

}
