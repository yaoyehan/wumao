package com.yyh.gulimall.search.controller;

import com.yyh.gulimall.search.service.MallSearchService;
import com.yyh.gulimall.search.service.ProductSaveService;
import com.yyh.gulimall.search.vo.SearchParam;
import com.yyh.gulimall.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class SearchController {
    @Autowired
    MallSearchService mallSearchService;
    @Autowired
    ProductSaveService productSaveService;
    @GetMapping("/list.html")
    public String listPage(SearchParam param, Model model, HttpServletRequest request){

        param.set_queryString(request. getQueryString());
        SearchResult result = mallSearchService.search(param);
        model.addAttribute("result",result);
        return "list";
    }
}
