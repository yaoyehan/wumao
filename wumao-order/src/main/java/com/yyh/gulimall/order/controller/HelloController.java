package com.yyh.gulimall.order.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Controller
public class HelloController {
    @GetMapping("/{page}.html")
    public String page(@PathVariable("page") String page){
        return page;
    }
}
