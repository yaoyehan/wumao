package com.yyh.gulimall.ware.listener;

import com.alibaba.fastjson.TypeReference;
import com.rabbitmq.client.Channel;
import com.yyh.common.to.OrderTo;
import com.yyh.common.to.mq.StockDetailTo;
import com.yyh.common.to.mq.StockLockedTo;
import com.yyh.common.utils.R;
import com.yyh.gulimall.ware.Vo.OrderVo;
import com.yyh.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.yyh.gulimall.ware.entity.WareOrderTaskEntity;
import com.yyh.gulimall.ware.feign.OrderFeignService;
import com.yyh.gulimall.ware.service.WareOrderTaskDetailService;
import com.yyh.gulimall.ware.service.WareOrderTaskService;
import com.yyh.gulimall.ware.service.WareSkuService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
@Service
@RabbitListener(queues = "stock.release.stock.queue")
public class StockReleaseListener {
    @Autowired
    WareOrderTaskDetailService wareOrderTaskDetailService;
    @Autowired
    WareOrderTaskService wareOrderTaskService;

    @Autowired
    WareSkuService wareSkuService;

    @RabbitHandler
    public void handleStockLockedRelease(StockLockedTo to, Message message, Channel channel) throws IOException {
/*        System.out.println("收到超时解锁库存的消息");
        wareSkuService.unlockStock(to);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);*/
        try {
            wareSkuService.unlockStock(to);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }catch (Exception e){
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }
    }
    @RabbitHandler
    public void handleOrderCloseRelease(OrderTo to, Message message, Channel channel) throws IOException {
/*        System.out.println("收到订单解锁库存的消息");
        wareSkuService.unlockStock(to);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);*/
        try {
            wareSkuService.unlockStock(to);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }catch (Exception e){
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }
    }

}
