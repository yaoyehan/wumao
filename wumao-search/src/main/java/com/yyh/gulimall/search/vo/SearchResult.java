package com.yyh.gulimall.search.vo;

import com.yyh.common.to.es.SkuEsModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
@Data
public class SearchResult {
    //查询到的所有商品信息
    private List<SkuEsModel> products;
    /**
     * 分页信息
     */
    private Integer pageNum;//当前页码
    private Long total;//总记录数
    private int totalPage;//总页码
    private List<BrandVo> brandVos;//当前查询到的结果中包含的品牌
    private List<CatalogVo> catelogs;//当前查询到的结果所涉及的所有的分类信息
    private List<AttrVo> attrs;//当前查询到的结果中包含的所有属性
    private List<Integer> pageNavs;
    //============以上是返回给页面的所有信息
    //面包屑导航数据
    private List<NavVo> navs=new ArrayList<>();
    private List<Long> attrIds =new ArrayList<>();
    @Data
    public static class NavVo{
        private String navName;
        private String navValue;
        private String link;
    }

    @Data
    public static class BrandVo{
        private Long brandId;
        private String brandName;
        private String brandImg;
    }
    @Data
    public static class CatalogVo{
        private Long catalogId;
        private String catalogName;

    }
    @Data
    public static class AttrVo{
        private Long attrId;
        private String attrName;
        private List<String> attrValue;
    }
}
