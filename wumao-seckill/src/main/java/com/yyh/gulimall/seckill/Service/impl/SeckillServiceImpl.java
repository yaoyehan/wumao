package com.yyh.gulimall.seckill.Service.impl;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.yyh.common.to.mq.SeckillOrderTo;
import com.yyh.common.utils.R;
import com.yyh.common.vo.MemberRespVo;
import com.yyh.gulimall.seckill.Service.SeckillService;
import com.yyh.gulimall.seckill.feign.CouponFeignService;
import com.yyh.gulimall.seckill.feign.ProductFeignService;
import com.yyh.gulimall.seckill.inteceptor.LoginUserInterceptor;
import com.yyh.gulimall.seckill.to.SeckillSkuRedisTo;
import com.yyh.gulimall.seckill.vo.SeckillSessionWithSkusVo;
import com.yyh.gulimall.seckill.vo.SeckillSkuVo;
import com.yyh.gulimall.seckill.vo.SkuInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {
    @Autowired
    CouponFeignService couponFeignService;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    RedissonClient redissonClient;

    private final String SESSION_CACHE_PREFIX = "seckill:sessions:";

    private final String SECKILL_CHARE_PREFIX = "seckill:skus";

    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:";    //+商品随机码

    @Override
    public void uploadSeckillSkuLatest3Days() {
        R session = couponFeignService.getLates3DaySession();
        if(session.getCode()==0){
            //上架商品
            List<SeckillSessionWithSkusVo> sessionData = session.getData(new TypeReference<List<SeckillSessionWithSkusVo>>() {
            });
            //缓冲到redis
            //1.缓存活动信息
            saveSessionInfos(sessionData);
            //2.缓存活动的关联商品信息
            saveSessionSkuInfo(sessionData);

        }
    }

    public List<SeckillSkuRedisTo> blockHandler(BlockException e) {
        log.error("getCurrentSeckillSkus方法被降级了");
        System.out.println("1111111111111");
        return null;
    }

    @SentinelResource(value = "getCurrentSeckillSkusResource",blockHandler = "blockHandler")
    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("222222222222222");
        //1、确定当前属于哪个秒杀场次
            long currentTime = System.currentTimeMillis();
            try(Entry entry=SphU.entry("seckillSkus")) {
                //从Redis中查询到所有key以seckill:sessions开头的所有数据
                Set<String> keys = stringRedisTemplate.keys(SESSION_CACHE_PREFIX + "*");
                for (String key : keys) {
                    //seckill:sessions:1594396764000_1594453242000
                    String replace = key.replace(SESSION_CACHE_PREFIX, "");
                    String[] s = replace.split("_");
                    //获取存入Redis商品的开始时间
                    long startTime = Long.parseLong(s[0]);
                    //获取存入Redis商品的结束时间
                    long endTime = Long.parseLong(s[1]);

                    //判断是否是当前秒杀场次
                    if (currentTime >= startTime && currentTime <= endTime) {
                        //2、获取这个秒杀场次需要的所有商品信息
                        List<String> range = stringRedisTemplate.opsForList().range(key, 0, -1);
                        BoundHashOperations<String, String, String> hasOps = stringRedisTemplate.boundHashOps(SECKILL_CHARE_PREFIX);
                        List<String> listValue = hasOps.multiGet(range);
                        if (listValue != null && listValue.size() >= 0) {

                            List<SeckillSkuRedisTo> collect = listValue.stream().map(item -> {
                                String items = (String) item;
                                SeckillSkuRedisTo redisTo = JSON.parseObject(items, SeckillSkuRedisTo.class);
                                // redisTo.setRandomCode(null);当前秒杀开始需要随机码
                                return redisTo;
                            }).collect(Collectors.toList());
                            return collect;
                        }
                        break;
                    }
                }
            }catch (BlockException e) {
                log.error("资源被限制，{}",e.getMessage());
            }
        return null;
    }

    private void saveSessionInfos(List<SeckillSessionWithSkusVo> sessionData) {
        if(sessionData!=null){
            sessionData.stream().forEach(session -> {
                Long startTime = session.getStartTime().getTime();
                Long endTime = session.getEndTime().getTime();
                String key=SESSION_CACHE_PREFIX+startTime+"_"+endTime;
                //缓存活动信息
                Boolean hasKey = stringRedisTemplate.hasKey(key);
                if(!hasKey){
                    List<String> collect = session.getRelationEntities().stream().map(item->item.getPromotionSessionId()+"-"+item.getSkuId().toString()).collect(Collectors.toList());
                    stringRedisTemplate.opsForList().leftPushAll(key,collect);
                }
            });
        }
    }


    /**
     * 缓存秒杀活动所关联的商品信息
     * @param sessions
     */
    private void saveSessionSkuInfo(List<SeckillSessionWithSkusVo> sessions) {
        if(sessions!=null){
            sessions.stream().forEach(session -> {
                //准备hash操作，绑定hash
                BoundHashOperations<String, Object, Object> operations = stringRedisTemplate.boundHashOps(SECKILL_CHARE_PREFIX);
                session.getRelationEntities().stream().forEach(seckillSkuVo -> {
                    //生成随机码
                    String token = UUID.randomUUID().toString().replace("-", "");
                    String redisKey = seckillSkuVo.getPromotionSessionId().toString() + "-" + seckillSkuVo.getSkuId().toString();
                    if (!operations.hasKey(redisKey)) {

                        //缓存我们商品信息
                        SeckillSkuRedisTo redisTo = new SeckillSkuRedisTo();
                        Long skuId = seckillSkuVo.getSkuId();
                        //1、先查询sku的基本信息，调用远程服务
                        R info = productFeignService.getSkuInfo(skuId);
                        if (info.getCode() == 0) {
                            SkuInfoVo skuInfo = info.getData("skuInfo",new TypeReference<SkuInfoVo>(){});
                            redisTo.setSkuInfo(skuInfo);
                        }

                        //2、sku的秒杀信息
                        BeanUtils.copyProperties(seckillSkuVo,redisTo);

                        //3、设置当前商品的秒杀时间信息
                        redisTo.setStartTime(session.getStartTime().getTime());
                        redisTo.setEndTime(session.getEndTime().getTime());

                        //4、设置商品的随机码（防止恶意攻击）
                        redisTo.setRandomCode(token);

                        //序列化json格式存入Redis中
                        String seckillValue = JSON.toJSONString(redisTo);
                        operations.put(seckillSkuVo.getPromotionSessionId().toString() + "-" + seckillSkuVo.getSkuId().toString(),seckillValue);

                        //如果当前这个场次的商品库存信息已经上架就不需要上架
                        //5、使用库存作为分布式Redisson信号量（限流）
                        // 使用库存作为分布式信号量
                        RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                        // 商品可以秒杀的数量作为信号量
                        semaphore.trySetPermits(seckillSkuVo.getSeckillCount());
                    }
                });
            });
        }


    }
    /**
     * 根据skuId查询商品是否参加秒杀活动
     * @param skuId
     * @return
     */
    @Override
    public SeckillSkuRedisTo getSkuSeckilInfo(Long skuId) {

        //1、找到所有需要秒杀的商品的key信息---seckill:skus
        BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SECKILL_CHARE_PREFIX);

        //拿到所有的key
        Set<String> keys = hashOps.keys();
        if (keys != null && keys.size() > 0) {
            //4-45 正则表达式进行匹配
            String reg = "\\d-" + skuId;
            for (String key : keys) {
                //如果匹配上了
                if (Pattern.matches(reg,key)) {
                    //从Redis中取出数据来
                    String redisValue = hashOps.get(key);
                    //进行序列化
                    SeckillSkuRedisTo redisTo = JSON.parseObject(redisValue, SeckillSkuRedisTo.class);

                    //随机码
                    Long currentTime = System.currentTimeMillis();
                    Long startTime = redisTo.getStartTime();
                    Long endTime = redisTo.getEndTime();
                    //如果当前时间大于等于秒杀活动开始时间并且要小于活动结束时间
                    if (currentTime >= startTime && currentTime <= endTime) {
                        return redisTo;
                    }
                    redisTo.setRandomCode(null);
                    return redisTo;
                }
            }
        }
        return null;
    }

    @Override
    public String kill(String killId, String key, Integer num) throws InterruptedException {

        long s1 = System.currentTimeMillis();
        //获取当前用户的信息
        MemberRespVo user = LoginUserInterceptor.loginUser.get();

        //1、获取当前秒杀商品的详细信息从Redis中获取
        BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SECKILL_CHARE_PREFIX);
        String skuInfoValue = hashOps.get(killId);
        if (StringUtils.isEmpty(skuInfoValue)) {
            return null;
        }
        //(合法性效验)
        SeckillSkuRedisTo redisTo = JSON.parseObject(skuInfoValue, SeckillSkuRedisTo.class);
        Long startTime = redisTo.getStartTime();
        Long endTime = redisTo.getEndTime();
        long currentTime = System.currentTimeMillis();
        //判断当前这个秒杀请求是否在活动时间区间内(效验时间的合法性)
        if (currentTime >= startTime && currentTime <= endTime) {

            //2、效验随机码和商品id
            String randomCode = redisTo.getRandomCode();
            String skuId = redisTo.getPromotionSessionId() + "-" +redisTo.getSkuId();
            if (randomCode.equals(key) && killId.equals(skuId)) {
                //3、验证购物数量是否合理和库存量是否充足
                Integer seckillLimit = redisTo.getSeckillLimit();

                //获取信号量
                String seckillCount = stringRedisTemplate.opsForValue().get(SKU_STOCK_SEMAPHORE + randomCode);
                Integer count = Integer.valueOf(seckillCount);
                //判断信号量是否大于0,并且买的数量不能超过库存
                if (count > 0 && num <= seckillLimit && count > num ) {
                    //4、验证这个人是否已经买过了（幂等性处理）,如果秒杀成功，就去占位。userId-sessionId-skuId
                    //SETNX 原子性处理
                    String redisKey = user.getId() + "-" + skuId;
                    //设置自动过期(活动结束时间-当前时间)
                    Long ttl = endTime - currentTime;
                    Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, num.toString(), ttl, TimeUnit.MILLISECONDS);
                    if (aBoolean) {
                        //占位成功说明从来没有买过,分布式锁(获取信号量-1)
                        RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode);
                        //TODO 秒杀成功，快速下单
                        boolean semaphoreCount = semaphore.tryAcquire(num, 100, TimeUnit.MILLISECONDS);
                        //保证Redis中还有商品库存
                        if (semaphoreCount) {
                            //创建订单号和订单信息发送给MQ
                            // 秒杀成功 快速下单 发送消息到 MQ 整个操作时间在 10ms 左右
                            String timeId = IdWorker.getTimeId();
                            SeckillOrderTo orderTo = new SeckillOrderTo();
                            orderTo.setOrderSn(timeId);
                            orderTo.setMemberId(user.getId());
                            orderTo.setNum(num);
                            orderTo.setPromotionSessionId(redisTo.getPromotionSessionId());
                            orderTo.setSkuId(redisTo.getSkuId());
                            orderTo.setSeckillPrice(redisTo.getSeckillPrice());
                            rabbitTemplate.convertAndSend("order-event-exchange","order.seckill.order",orderTo);

                            long s2 = System.currentTimeMillis();
                            log.info("耗时..." + (s2 - s1));
                            return timeId;
                        }
                    }
                }
            }
        }
        long s3 = System.currentTimeMillis();
        log.info("耗时..." + (s3 - s1));
        return null;
    }

}
