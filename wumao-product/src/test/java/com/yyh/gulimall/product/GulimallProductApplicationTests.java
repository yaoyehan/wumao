package com.yyh.gulimall.product;

import com.alibaba.nacos.common.utils.UuidUtils;
import com.yyh.gulimall.product.dao.AttrGroupDao;
import com.yyh.gulimall.product.dao.SkuSaleAttrValueDao;
import com.yyh.gulimall.product.service.AttrGroupService;
import com.yyh.gulimall.product.service.CategoryService;
import com.yyh.gulimall.product.vo.SkuItemVo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;


import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;


@Slf4j
@SpringBootTest
class GulimallProductApplicationTests {
    @Autowired
    CategoryService categoryService;
    @Autowired
    AttrGroupDao attrGroupDao;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    RedissonClient redissonClient;
    @Autowired
    SkuSaleAttrValueDao skuSaleAttrValueDao;
    @Test
    public void testUpload() throws Exception{
        Long[] catelogPath = categoryService.findcatalogPath(225L);
        log.info("完整路径{}", Arrays.asList(catelogPath));
    }
    @Test
    public void setStringRedisTemplate(){
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        ops.set("hello","world"+ UUID.randomUUID().toString());
        String hello=ops.get("hello");
        System.out.println(hello);
    }
    @Test
    public void redisson(){
        System.out.println(redissonClient);
    }

    @Test
    void contextLoads() {
        List<SkuItemVo.SpuItemAttrGroupVo> attrGroupWithAttrsBySpuId = attrGroupDao.getAttrGroupWithAttrsBySpuId(14L, 225L);
        System.out.println(attrGroupWithAttrsBySpuId);

    }
    @Test
    void AttrSale(){
        List<SkuItemVo.SpuItemSaleAttrsVo> saleAttrsBySpuId = skuSaleAttrValueDao.getSaleAttrsBySpuId(14L);
        System.out.println(saleAttrsBySpuId.toString());
    }


}
