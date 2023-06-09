package com.yyh.gulimall.product.web;

import com.yyh.gulimall.product.entity.CategoryEntity;
import com.yyh.gulimall.product.service.CategoryService;
import com.yyh.gulimall.product.vo.Catalog2Vo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Controller
public class IndexController {
    @Autowired
    CategoryService categoryService;
    @GetMapping({"/","index.html"})
    public String indexPage(Model model){
        List<CategoryEntity> categoryEntities=categoryService.getLevel1Categorys();
        model.addAttribute("categorys",categoryEntities);
        return "index";
    }
    @ResponseBody
    @GetMapping("/index/catalog.json")
    public Map<String, List<Catalog2Vo>> getCatalogJson(){
        Map<String, List<Catalog2Vo>> catalogJson = categoryService.getCatalogJson();
        return catalogJson;
    }

}
