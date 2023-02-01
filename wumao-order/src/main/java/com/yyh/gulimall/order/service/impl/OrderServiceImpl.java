package com.yyh.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.alibaba.nacos.common.utils.UuidUtils;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.yyh.common.exception.NoStockException;
import com.yyh.common.to.OrderTo;
import com.yyh.common.to.mq.SeckillOrderTo;
import com.yyh.common.utils.R;
import com.yyh.common.vo.MemberRespVo;
import com.yyh.gulimall.order.OrderStatusEnum;
import com.yyh.gulimall.order.constant.OrderConstant;
import com.yyh.gulimall.order.dao.OrderItemDao;
import com.yyh.gulimall.order.entity.OrderItemEntity;
import com.yyh.gulimall.order.entity.OrderReturnReasonEntity;
import com.yyh.gulimall.order.entity.PaymentInfoEntity;
import com.yyh.gulimall.order.feign.CartFeignService;
import com.yyh.gulimall.order.feign.MemberFeignService;
import com.yyh.gulimall.order.feign.ProductFeignService;
import com.yyh.gulimall.order.feign.WmsFeignService;
import com.yyh.gulimall.order.inteceptor.LoginUserInterceptor;
import com.yyh.gulimall.order.service.OrderItemService;
import com.yyh.gulimall.order.service.PaymentInfoService;
import com.yyh.gulimall.order.to.OrderCreateTo;
import com.yyh.gulimall.order.to.SpuInfoVo;
import com.yyh.gulimall.order.vo.*;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.commons.io.CopyUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yyh.common.utils.PageUtils;
import com.yyh.common.utils.Query;

import com.yyh.gulimall.order.dao.OrderDao;
import com.yyh.gulimall.order.entity.OrderEntity;
import com.yyh.gulimall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {
    ThreadLocal<OrderSubmitVo> confirmVoThreadLocal=new ThreadLocal<>();
    @Autowired
    OrderDao orderDao;
    @Autowired
    OrderItemDao orderItemDao;
    @Autowired
    PaymentInfoService paymentInfoService;
    @Autowired
    OrderItemService orderItemService;
    @Autowired
    MemberFeignService memberFeignService;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    CartFeignService cartFeignService;
    @Autowired
    ThreadPoolExecutor executor;
    @Autowired
    WmsFeignService wmsFeignService;
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    RabbitTemplate rabbitTemplate;
    public final String CART_PREFIX="gulimall:cart:";
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            //远程查询用户的地址
            List<MemberAddressVo> address = memberFeignService.getAddress(memberRespVo.getId());
            orderConfirmVo.setMemberAddressVos(address);
        }, executor);
        CompletableFuture<Void> getOrderItemVoFuture = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            //远程查询用的的购物车的购物项
            List<OrderItemVo> items = cartFeignService.getCurrentCartItems();
            orderConfirmVo.setItems(items);
        }, executor).thenRunAsync(()->{
            List<OrderItemVo> items = orderConfirmVo.getItems();
            List<Long> collect = items.stream().map((item) -> {
                return item.getSkuId();
            }).collect(Collectors.toList());
            R skuHasStock = wmsFeignService.getSkuHasStock(collect);
            List<SkuStockVo> data = skuHasStock.getData(new TypeReference<List<SkuStockVo>>() {
            });
            if(data!=null){
                Map<Long, Boolean> collect1 = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                orderConfirmVo.setStocks(collect1);
            }

        },executor);
        //3.查询用户积分
        Integer integration = memberRespVo.getIntegration();
        orderConfirmVo.setIntegration(integration);
        //4、设置防重令牌
        String token = UUID.randomUUID().toString().replace("-","");
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX+memberRespVo.getId(),token,30, TimeUnit.MINUTES);
        CompletableFuture.allOf(getAddressFuture,getOrderItemVoFuture).get();
        orderConfirmVo.setOrderToken(token);
        return orderConfirmVo;
    }
/*    @GlobalTransactional*/
    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo orderSubmitVo) {
        confirmVoThreadLocal.set(orderSubmitVo);
        SubmitOrderResponseVo responseVo = new SubmitOrderResponseVo();
        responseVo.setCode(0);
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        //1.验证令牌【令牌的对比和删除必须保证原子性】
        String orderToken = orderSubmitVo.getOrderToken();
        String script="if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        //原子验证令牌和删除令牌
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),
                Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId()),
                orderToken);
        if(result==0L){
            //令牌验证错误
            responseVo.setCode(1);
            return responseVo;
        }else {
            //令牌验证成功
            OrderCreateTo order = createOrder();
            //验证价格
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = orderSubmitVo.getPayPrice();
            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
                //金额对比
                //TODO 3、保存订单
                saveOrder(order);
                //4.库存锁定
                WareSkuLockVo wareSkuLockVo = new WareSkuLockVo();
                wareSkuLockVo.setOrderSn(order.getOrder().getOrderSn());
                List<OrderItemVo> locks = order.getOrderItems().stream().map(item -> {
                    OrderItemVo orderItemVo = new OrderItemVo();
                    orderItemVo.setSkuId(item.getSkuId());
                    orderItemVo.setCount(item.getSkuQuantity());
                    orderItemVo.setTitle(item.getSkuName());
                    return orderItemVo;
                }).collect(Collectors.toList());
                wareSkuLockVo.setLocks(locks);
                //远程锁库存
                R r = wmsFeignService.orderLockStock(wareSkuLockVo);
                if(r.getCode()==0){
                    //锁成功
                    responseVo.setOrder(order.getOrder());
                    //TODO 订单创建完成发消息给rabbitmq
                    //TODO 订单创建成功，发送消息给MQ
                    rabbitTemplate.convertAndSend("order-event-exchange","order.create.order",order.getOrder());

                    //删除购物车里的数据
                    redisTemplate.delete(CART_PREFIX+memberRespVo.getId());
                    return responseVo;
                }else {
                    //锁定失败
                    String msg = (String) r.get("msg");
                    responseVo.setCode(3);
                    throw new NoStockException(msg);
                }
            }else {
                responseVo.setCode(2);
                return responseVo;
            }

        }
/*
        String redisToken = redisTemplate.opsForValue().get(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId());
        if(orderToken!=null&&orderToken.equals(redisToken)){
            //令牌验证通过

        }else {
            //令牌验证不通过
        };
*/
    }

    @Override
    public OrderEntity getOrderStatusByOrderSn(String orderSn) {
        OrderEntity orderEntity = this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
        return orderEntity;
    }

    @Override
    public void closeOrder(OrderEntity orderEntity) {

        //关闭订单之前先查询一下数据库，判断此订单状态是否已支付
        OrderEntity orderInfo = this.getOne(new QueryWrapper<OrderEntity>().
                eq("order_sn",orderEntity.getOrderSn()));
        if (orderInfo.getStatus().equals(OrderStatusEnum.CREATE_NEW.getCode())) {
            //代付款状态进行关单
            OrderEntity orderUpdate = new OrderEntity();
            orderUpdate.setId(orderInfo.getId());
            orderUpdate.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(orderUpdate);

            // 发送消息给MQ
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(orderInfo, orderTo);

            try {
                //TODO 确保每个消息发送成功，给每个消息做好日志记录，(给数据库保存每一个详细信息)保存每个消息的详细信息
                rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other", orderTo);
            } catch (Exception e) {
                //TODO 定期扫描数据库，重新发送失败的消息
                System.out.println(e.getMessage());
            }
        }
    }

    /**
     * 获取当前订单的支付信息
     * @param orderSn
     * @return
     */
    @Override
    public PayVo getOrderPay(String orderSn) {
        PayVo payVo = new PayVo();
        OrderEntity order = this.getOrderStatusByOrderSn(orderSn);
        BigDecimal bigDecimal = order.getPayAmount().setScale(2, BigDecimal.ROUND_UP);
        payVo.setTotal_amount(bigDecimal.toString());
        payVo.setOut_trade_no(order.getOrderSn());
        List<OrderItemEntity> order_sn = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        OrderItemEntity entity = order_sn.get(0);
        payVo.setSubject(entity.getSkuName());
        payVo.setBody(entity.getSkuAttrsVals());

        return payVo;
    }

    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {

        MemberRespVo memberResponseVo = LoginUserInterceptor.loginUser.get();

        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
                        .eq("member_id",memberResponseVo.getId()).orderByDesc("create_time")
        );

        //遍历所有订单集合
        List<OrderEntity> orderEntityList = page.getRecords().stream().map(order -> {
            //根据订单号查询订单项里的数据
            List<OrderItemEntity> orderItemEntities = orderItemService.list(new QueryWrapper<OrderItemEntity>()
                    .eq("order_sn", order.getOrderSn()));
            order.setOrderItemEntities(orderItemEntities);
            return order;
        }).collect(Collectors.toList());

        page.setRecords(orderEntityList);

        return new PageUtils(page);
    }

    @Override
    public String handlePayResult(PayAsyncVo asyncVo) {

        //保存交易流水信息
        PaymentInfoEntity paymentInfo = new PaymentInfoEntity();
        paymentInfo.setOrderSn(asyncVo.getOut_trade_no());
        paymentInfo.setAlipayTradeNo(asyncVo.getTrade_no());
        paymentInfo.setTotalAmount(new BigDecimal(asyncVo.getBuyer_pay_amount()));
        paymentInfo.setSubject(asyncVo.getBody());
        paymentInfo.setPaymentStatus(asyncVo.getTrade_status());
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setCallbackTime(asyncVo.getNotify_time());
        //添加到数据库中
        paymentInfoService.save(paymentInfo);

        //修改订单状态
        //获取当前状态
        String tradeStatus = asyncVo.getTrade_status();

        if (tradeStatus.equals("TRADE_SUCCESS") || tradeStatus.equals("TRADE_FINISHED")) {
            //支付成功状态
            String orderSn = asyncVo.getOut_trade_no(); //获取订单号
            this.baseMapper.updateOrderStatus(orderSn,OrderStatusEnum.PAYED.getCode());
        }
        return "success";
    }

    /**
     * 创建秒杀单
     * @param orderTo
     */
    @Override
    public void createSeckillOrder(SeckillOrderTo orderTo) {

        //TODO 保存订单信息
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(orderTo.getOrderSn());
        orderEntity.setMemberId(orderTo.getMemberId());
        orderEntity.setCreateTime(new Date());
        BigDecimal totalPrice = orderTo.getSeckillPrice().multiply(BigDecimal.valueOf(orderTo.getNum()));
        orderEntity.setPayAmount(totalPrice);
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());

        //保存订单
        this.save(orderEntity);

        //保存订单项信息
        OrderItemEntity orderItem = new OrderItemEntity();
        orderItem.setOrderSn(orderTo.getOrderSn());
        orderItem.setRealAmount(totalPrice);

        orderItem.setSkuQuantity(orderTo.getNum());

        //保存商品的spu信息
        R spuInfo = productFeignService.getSpuInfoBySkuId(orderTo.getSkuId());
        SpuInfoVo spuInfoData = spuInfo.getData("data", new TypeReference<SpuInfoVo>() {
        });
        orderItem.setSpuId(spuInfoData.getId());
        orderItem.setSpuName(spuInfoData.getSpuName());
        orderItem.setSpuBrand(spuInfoData.getBrandName());
        orderItem.setCategoryId(spuInfoData.getCatalogId());

        //保存订单项数据
        orderItemService.save(orderItem);
    }


    /**
     *  保存订单
     * @param order
     */
    private void saveOrder(OrderCreateTo order) {
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        orderEntity.setCreateTime(new Date());
        orderEntity.setStatus(0);

        this.save(orderEntity);
        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItemService.saveBatch(orderItems);

    }

    private OrderCreateTo createOrder() {
        //生成订单
        OrderCreateTo orderCreateTo = new OrderCreateTo();
        //生成订单号
        String orderSn = IdWorker.getTimeId();
        OrderEntity orderEntity = buildOrder(orderSn);
        //获取所有的订单项信息
        List<OrderItemEntity> orderItemEntities = buildOrderItems(orderSn);
        //验价(计算价格、积分等信息)
        computePrice(orderEntity,orderItemEntities);
        orderCreateTo.setOrderItems(orderItemEntities);
        orderCreateTo.setOrder(orderEntity);
        return orderCreateTo;
    }

    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> orderItemEntities) {
        BigDecimal total = new BigDecimal("0.0");
        //优惠价
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal intergration = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");

        //积分、成长值
        Integer integrationTotal = 0;
        Integer growthTotal = 0;

        for (OrderItemEntity entity : orderItemEntities) {
            //优惠价格信息
            coupon = coupon.add(entity.getCouponAmount());
            promotion = promotion.add(entity.getPromotionAmount());
            intergration = intergration.add(entity.getIntegrationAmount());
            //总价
            total = total.add(entity.getRealAmount());
            //积分信息和成长值信息
            integrationTotal += entity.getGiftIntegration();
            growthTotal += entity.getGiftGrowth();
        }
        //1.订单总额
        orderEntity.setTotalAmount(total);
        //设置应付总额(总额+运费)
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        orderEntity.setCouponAmount(coupon);
        orderEntity.setPromotionAmount(promotion);
        orderEntity.setIntegrationAmount(intergration);
        //设置订单状态
        //设置积分成长值信息
        orderEntity.setIntegration(integrationTotal);
        orderEntity.setGrowth(growthTotal);
        orderEntity.setDeleteStatus(0);//未删除
    }

    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        //最后确定每个购物项的价格
        List<OrderItemVo> currentCartItems = cartFeignService.getCurrentCartItems();
        if(currentCartItems!=null&&currentCartItems.size()>0){
            List<OrderItemEntity> itemEntities = currentCartItems.stream().map(cartItem -> {
                OrderItemEntity orderItemEntity = buildOrderItem(cartItem);
                orderItemEntity.setOrderSn(orderSn);
                return orderItemEntity;
            }).collect(Collectors.toList());
            return itemEntities;
        }
        return null;
    }

    /**
     * 构建某一个订单项
     * @param orderSn
     * @return
     */
    private OrderEntity buildOrder(String orderSn) {
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        //创建订单号
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(orderSn);
        orderEntity.setMemberId(memberRespVo.getId());
        //获取收货地址信息
        OrderSubmitVo submitVo = confirmVoThreadLocal.get();
        R fare = wmsFeignService.getFare(submitVo.getAddrId());
        FareVo fareData = fare.getData(new TypeReference<FareVo>() {
        });
        //设置运费信息
        orderEntity.setFreightAmount(fareData.getFare());
        //设置收货人地址信息
        orderEntity.setReceiverProvince(fareData.getAddress().getProvince());
        orderEntity.setReceiverCity(fareData.getAddress().getCity());
        orderEntity.setReceiverRegion(fareData.getAddress().getRegion());
        orderEntity.setReceiverDetailAddress(fareData.getAddress().getDetailAddress());
        //设置收货人信息
        orderEntity.setReceiverName(fareData.getAddress().getName());
        orderEntity.setReceiverPhone(fareData.getAddress().getPhone());
        //设置收货人的其他信息
        orderEntity.setReceiverPostCode(fareData.getAddress().getPostCode());
        return orderEntity;
    }

    private OrderItemEntity buildOrderItem(OrderItemVo items) {

        OrderItemEntity orderItemEntity = new OrderItemEntity();

        //1、商品的spu信息
        Long skuId = items.getSkuId();
        //获取spu的信息
        R spuInfo = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo spuInfoData = spuInfo.getData("data", new TypeReference<SpuInfoVo>() {
        });
        orderItemEntity.setSpuId(spuInfoData.getId());
        orderItemEntity.setSpuName(spuInfoData.getSpuName());
        orderItemEntity.setSpuBrand(spuInfoData.getBrandName());
        orderItemEntity.setCategoryId(spuInfoData.getCatalogId());

        //2、商品的sku信息
        orderItemEntity.setSkuId(skuId);
        orderItemEntity.setSkuName(items.getTitle());
        orderItemEntity.setSkuPic(items.getImage());
        orderItemEntity.setSkuPrice(items.getPrice());
        orderItemEntity.setSkuQuantity(items.getCount());

        //使用StringUtils.collectionToDelimitedString将list集合转换为String
        String skuAttrValues = StringUtils.collectionToDelimitedString(items.getSkuAttrValues(), ";");
        orderItemEntity.setSkuAttrsVals(skuAttrValues);

        //3、商品的优惠信息

        //4、商品的积分信息
        orderItemEntity.setGiftGrowth(items.getPrice().multiply(new BigDecimal(items.getCount())).intValue());
        orderItemEntity.setGiftIntegration(items.getPrice().multiply(new BigDecimal(items.getCount())).intValue());

        //5、订单项的价格信息
        orderItemEntity.setPromotionAmount(BigDecimal.ZERO);
        orderItemEntity.setCouponAmount(BigDecimal.ZERO);
        orderItemEntity.setIntegrationAmount(BigDecimal.ZERO);

        //当前订单项的实际金额.总额 - 各种优惠价格
        //原来的价格
        BigDecimal origin = orderItemEntity.getSkuPrice().multiply(new BigDecimal(orderItemEntity.getSkuQuantity().toString()));
        //原价减去优惠价得到最终的价格
        BigDecimal subtract = origin.subtract(orderItemEntity.getCouponAmount())
                .subtract(orderItemEntity.getPromotionAmount())
                .subtract(orderItemEntity.getIntegrationAmount());
        orderItemEntity.setRealAmount(subtract);

        return orderItemEntity;
    }
}