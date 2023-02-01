package com.yyh.gulimall.search.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class SearchParam {
    private String keyword;//全文匹配关键字
    private Long catalog3Id;//三级分类Id
    private String sort;//排序条件
    private Integer hasStock=1;//有库存，0无库存，1又库存
    private String skuPrice;//价格区间查询
    private List<Long> brandId;//品牌id
    private List<String> attrs;//根据属性筛选
    private Integer pageNum=1;//页码
    private String _queryString;//原生的查询条件
}
