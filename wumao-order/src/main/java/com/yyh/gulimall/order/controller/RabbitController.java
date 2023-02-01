package com.yyh.gulimall.order.controller;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RabbitController {
    @Autowired
    RabbitTemplate rabbitTemplate;

    @GetMapping("send")
    public String sendMessage(){
        for (int i = 0; i < 10; i++) {
            rabbitTemplate.convertAndSend("directExchange","gulimall-queue","hahaha-"+i);
        }
        return "ok";
    }
}
