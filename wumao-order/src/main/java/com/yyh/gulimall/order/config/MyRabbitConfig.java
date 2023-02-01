package com.yyh.gulimall.order.config;

import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.MessageConverter;

import javax.annotation.PostConstruct;

@Configuration
public class MyRabbitConfig {
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }
    @PostConstruct
    public void initRabbitTemplate(){
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            @Override
            public void confirm(CorrelationData correlationData, boolean ack, String cause) {
                System.out.println("confirm...correlationData["+correlationData+"]==>ack["+ack+"]==>cause["+cause+"]");
            }
        });
        rabbitTemplate.setReturnsCallback(new RabbitTemplate.ReturnsCallback() {
            @Override
            public void returnedMessage(ReturnedMessage returned) {
                System.out.println("Message==>["+returned.getMessage()+
                        "]Exchange==>["+returned.getExchange()+"]"+
                        "RoutingKey==>["+returned.getRoutingKey()+"]"+
                        "ReplyText==>["+returned.getReplyText()+"]"+
                        "ReplyCode==>["+returned.getReplyCode()+"]");
            }
        });
    }

}
