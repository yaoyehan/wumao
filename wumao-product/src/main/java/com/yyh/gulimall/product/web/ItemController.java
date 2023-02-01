package com.yyh.gulimall.product.web;

import com.yyh.gulimall.product.service.SkuInfoService;
import com.yyh.gulimall.product.vo.SkuItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.concurrent.ExecutionException;

@Controller
public class ItemController {
    @Autowired
    SkuInfoService skuInfoService;

    @GetMapping("/{skuId}.html")
    public String skuItem(@PathVariable("skuId") String skuId, Model model) throws ExecutionException, InterruptedException {
        System.out.println("准备查询"+skuId+"详情");
        SkuItemVo item=skuInfoService.item(skuId);
        model.addAttribute("item",item);
        return "item";
    }
}
