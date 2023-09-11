package com.yyh.gulimall.order;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GulimallOrderApplicationTests {

    @Test
    void contextLoads() {
        String timeId = IdWorker.getTimeId();
        System.out.println(timeId);
    }

}
