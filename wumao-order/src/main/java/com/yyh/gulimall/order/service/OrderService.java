package com.yyh.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yyh.common.to.mq.SeckillOrderTo;
import com.yyh.common.utils.PageUtils;
import com.yyh.gulimall.order.entity.OrderEntity;
import com.yyh.gulimall.order.vo.*;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 订单
 *
 * @author yyh
 * @email 1697149091@qq.com
 * @date 2022-06-16 16:21:09
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 订单确认页返回需要用到的数据
     * @return
     */
    OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException;

    SubmitOrderResponseVo submitOrder(OrderSubmitVo orderSubmitVo);

    OrderEntity getOrderStatusByOrderSn(String orderSn);

    void closeOrder(OrderEntity orderEntity);

    PayVo getOrderPay(String orderSn);

    PageUtils queryPageWithItem(Map<String, Object> params);

    String handlePayResult(PayAsyncVo asyncVo);

    void createSeckillOrder(SeckillOrderTo orderTo);
}

