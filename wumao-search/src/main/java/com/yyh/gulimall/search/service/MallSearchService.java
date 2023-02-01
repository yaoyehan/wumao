package com.yyh.gulimall.search.service;

import com.yyh.gulimall.search.vo.SearchParam;
import com.yyh.gulimall.search.vo.SearchResult;

public interface MallSearchService {
    SearchResult search(SearchParam param);
}
