package com.yyh.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.yyh.common.to.es.SkuEsModel;
import com.yyh.common.utils.R;
import com.yyh.gulimall.search.config.GulimallElasticSearchConfig;
import com.yyh.gulimall.search.constant.EsConstant;
import com.yyh.gulimall.search.feign.ProductFeignService;
import com.yyh.gulimall.search.service.MallSearchService;
import com.yyh.gulimall.search.service.ProductSaveService;
import com.yyh.gulimall.search.vo.*;
import org.apache.lucene.search.TotalHits;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.elasticsearch.search.sort.SortOrder;
import javax.swing.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MallSearchServiceImpl implements MallSearchService {
    @Autowired
    RestHighLevelClient client;
    @Autowired
    ProductFeignService productFeignService;
    //检索的所有参数，返回所有结果
    @Override
    public SearchResult search(SearchParam param) {
        SearchResult result=null;
        //准备检索请求
        SearchRequest searchRequest=buildSearchRequest(param);

        try {
            SearchResponse search = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
            result=buildSearchResult(search,param);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 构建结果数据
     * @param response
     * @return
     */
    private SearchResult buildSearchResult(SearchResponse response,SearchParam param) {
        SearchResult result = new SearchResult();
        //1、返回所有查询到的商品
        SearchHits hits = response.getHits();
        List<SkuEsModel> products=new ArrayList<SkuEsModel>();
        if(hits.getHits()!=null&&hits.getHits().length>0){
            for (SearchHit hit : hits.getHits()) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel skuEsModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
                if(!StringUtils.isEmpty(param.getKeyword())){
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String string = skuTitle.getFragments()[0].string();
                    skuEsModel.setSkuTitle(string);
                }
                products.add(skuEsModel);
            }
        }
        result.setProducts(products);
        //2、当前所有商品所涉及到的所有属性信息
        List<SearchResult.AttrVo> attrVos=new ArrayList<>();
        ParsedNested attr_agg = response.getAggregations().get("attr_agg");
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");
        for (Terms.Bucket bucket : attr_id_agg.getBuckets()) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            //得到屬性的id
            long attrId = bucket.getKeyAsNumber().longValue();
            //得到属性的名字
            String attrName = ((ParsedStringTerms) bucket.getAggregations().get("attr_name_agg")).getBuckets().get(0).getKeyAsString();
            //获取属性的所有值
            List<String> attrValues = ((ParsedStringTerms) bucket.getAggregations().get("attr_value_agg")).getBuckets().stream().map(item -> {
                String keyAsString = item.getKeyAsString();
                return keyAsString;
            }).collect(Collectors.toList());
            attrVo.setAttrId(attrId);
            attrVo.setAttrName(attrName);
            attrVo.setAttrValue(attrValues);

            attrVos.add(attrVo);
        }
        result.setAttrs(attrVos);
        //3、当前所有商品所涉及到的所有品牌信息
        ArrayList<SearchResult.BrandVo> brandVos = new ArrayList<>();
        ParsedLongTerms brand_agg = response.getAggregations().get("brand_agg");
        for (Terms.Bucket bucket : brand_agg.getBuckets()) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            //得到品牌的id
            long brandId = bucket.getKeyAsNumber().longValue();
            //得到品牌的名字
            String brandName = ((ParsedStringTerms) bucket.getAggregations().get("brand_name_agg")).getBuckets().get(0).getKeyAsString();
            //得到品牌的图片
            String brandImg = ((ParsedStringTerms) bucket.getAggregations().get("brand_img_agg")).getBuckets().get(0).getKeyAsString();
            brandVo.setBrandId(brandId);
            brandVo.setBrandName(brandName);
            brandVo.setBrandImg(brandImg);
            brandVos.add(brandVo);
        }
        result.setBrandVos(brandVos);
        //4、当前所有商品所涉及到的所有分类信息
        ParsedLongTerms catalog_agg = response.getAggregations().get("catalog_agg");
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        List<? extends Terms.Bucket> buckets = catalog_agg.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            //得到分类id
            String keyAsString = bucket.getKeyAsString();
            catalogVo.setCatalogId(Long.parseLong(keyAsString));
            //获取分类名
            ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            String catalog_name = catalog_name_agg.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(catalog_name);
            catalogVos.add(catalogVo);
        }
        result.setCatelogs(catalogVos);
        //5、分页信息
        result.setPageNum(param.getPageNum());
        //6、总记录数
        long total = hits.getTotalHits().value;
        result.setTotal(total);
        //7、总页码
        int totalPage=(int) total%EsConstant.PRODUCT_PAGESIZE==0?(int) total/EsConstant.PRODUCT_PAGESIZE:(int) total/EsConstant.PRODUCT_PAGESIZE+1;
        result.setTotalPage(totalPage);
        ArrayList<Integer> pageNavs = new ArrayList<>();
        for (int i = 1;i<=totalPage;i++){
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);
        //构建面包屑导航数据
        if(param.getAttrs()!=null&&param.getAttrs().size()>0){
            List<SearchResult.NavVo> collect = param.getAttrs().stream().map(attr -> {
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                String[] s = attr.split("_");
                navVo.setNavValue(s[1]);
                R r = productFeignService.attrInfo(Long.parseLong(s[0]));
                result.getAttrIds().add(Long.parseLong(s[0]));
                if (r.getCode() == 0) {
                    AttrResponseVo data = r.getData("attr", new TypeReference<AttrResponseVo>() {
                    });
                    navVo.setNavName(data.getAttrName());
                } else {
                    navVo.setNavName(s[0]);
                }
                //取消面包屑后要跳转到哪个地方
                String replace = replaceQueryString(param, attr,"attrs");
                navVo.setLink("http://search.gulimall.com/list.html?"+replace);
                return navVo;
            }).collect(Collectors.toList());
            result.setNavs(collect);
        }
        //品牌
        if(param.getBrandId()!=null&&param.getBrandId().size()>0){

            List<SearchResult.NavVo> navs=null;
            if(!(result.getNavs()==null)){
                 navs= result.getNavs();
            }else {
                 navs= new ArrayList<>();
            }

            SearchResult.NavVo navVo = new SearchResult.NavVo();
            navVo.setNavName("品牌");
            //远程调用
            R r = productFeignService.BrandsInfo(param.getBrandId());
            if(r.getCode()==0){
                List<BrandVo> brand = r.getData("brand", new TypeReference<List<BrandVo>>() {
                });
                StringBuffer stringBuffer = new StringBuffer();
                String replace="";
                for (BrandVo brandVo : brand) {
                    stringBuffer.append(brandVo.getBrandName()+";");
                    replaceQueryString(param,brandVo.getBrandId()+"","brandId");
                }
                navVo.setNavValue(stringBuffer.toString());
                navVo.setLink("http://search.gulimall.com/list.html?"+replace);
            }
            navs.add(navVo);
        }
        //分类
        if(param.getCatalog3Id()!=null){
            List<SearchResult.NavVo> navs=null;
            if(!(result.getNavs()==null)){
                navs= result.getNavs();
            }else {
                navs= new ArrayList<>();
            }
            SearchResult.NavVo navVo = new SearchResult.NavVo();
            navVo.setNavName("分类");
            //远程调用
            R r = productFeignService.CatInfo(param.getCatalog3Id());
            if(r.getCode()==0){
                CatVo catVo = r.getData("data", new TypeReference<CatVo>() {
                });
                navVo.setNavValue(catVo.getName());
            }
            navs.add(navVo);
        }
        return result;
    }

    private String replaceQueryString(SearchParam param, String value,String key) {

        String encode=null;
        try {
            encode= URLEncoder.encode(value,"UTF-8");
            encode=encode.replace("+","%20");//浏览器与java对空格的编译方式不一样
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String queryString = param.get_queryString();
        String replace="";
        if(queryString.contains("&"+key+"="+encode)){
             replace= queryString.replace("&"+key+"=" + encode, "");
        }else if(queryString.contains(key+"="+encode+"&")){
            replace = queryString.replace(key+"=" + encode+"&", "");
        }else if(queryString.contains(key+"="+encode)){
            replace = queryString.replace(key+"=" + encode, "");
        }
        return replace;
    }

    /**
     * 准备检索请求
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParam searchParam) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();//构建DSL语句
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //  模糊查询 must
        if(!StringUtils.isEmpty(searchParam.getKeyword())){
            boolQueryBuilder.must(QueryBuilders.matchQuery("skuTitle",searchParam.getKeyword()));
        }
        //过滤分类
        if(searchParam.getCatalog3Id()!=null){
            boolQueryBuilder.filter(QueryBuilders.termQuery("catalogId",searchParam.getCatalog3Id()));
        }
        //过滤品牌id
        if(searchParam.getBrandId()!=null&&searchParam.getBrandId().size()>0){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId",searchParam.getBrandId()));
        }
        //按照所有指定的属性进行查询
        if(searchParam.getAttrs()!=null&&searchParam.getAttrs().size()>0){
            //attr=1_5寸:8寸&attr=2_8G:16G
            for (String attr : searchParam.getAttrs()) {
                BoolQueryBuilder boolQueryBuilder1 = QueryBuilders.boolQuery();
                //attr=1_5寸:8寸
                String[] s = attr.split("_");
                String attrId=s[0];//检索的属性id
                String[] attrValues = s[1].split(":");//检索的属性对应的属性值
                boolQueryBuilder1.must(QueryBuilders.termQuery("attrs.attrId",attrId));
                boolQueryBuilder1.must(QueryBuilders.termsQuery("attrs.attrValue",attrValues));
                NestedQueryBuilder nestedQueryBuilder = QueryBuilders.nestedQuery("attrs", boolQueryBuilder1, ScoreMode.None);
                boolQueryBuilder.filter(nestedQueryBuilder);
            }
        }
        //按照库存是否有进行查询
        boolQueryBuilder.filter(QueryBuilders.termQuery("hasStock",searchParam.getHasStock()==1));
        //按照价格区间进行查询
        if(!StringUtils.isEmpty(searchParam.getSkuPrice())){
            //1_500/_500/500_不同的价格·1区间
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            String[] s = searchParam.getSkuPrice().split("_");
            if(s.length==2){
                rangeQuery.gte(s[0]).lte(s[1]);
            }else if(s.length==1){
                if(searchParam.getSkuPrice().startsWith("_")){
                    rangeQuery.gte(s[0]);
                }
                if(searchParam.getSkuPrice().endsWith("_")){
                    rangeQuery.lte(s[0]);
                }
           }
            boolQueryBuilder.filter(rangeQuery);
        }
        searchSourceBuilder.query(boolQueryBuilder);
        /**
         * 排序，分页，高亮
         */
        //排序
        if(!StringUtils.isEmpty(searchParam.getSort())){
            String sort = searchParam.getSort();
            //sort=hostScore_asc/desc
            String[] s = sort.split("_");
            SortOrder sortOrder=s[1].equalsIgnoreCase("asc")?SortOrder.ASC:SortOrder.DESC;
            searchSourceBuilder.sort(s[0],sortOrder);
        }
        //分页
        searchSourceBuilder.from((searchParam.getPageNum()-1)*EsConstant.PRODUCT_PAGESIZE);
        searchSourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);
        //高亮
        if(!StringUtils.isEmpty(searchParam.getKeyword())){
            HighlightBuilder highlightBuilder=new HighlightBuilder();
            highlightBuilder.field("skuTitle");
            highlightBuilder.preTags("<b style='color:red'>");
            highlightBuilder.postTags("</b>");

            searchSourceBuilder.highlighter(highlightBuilder);
        }
        /**
         * 聚合分析
         */
        //1、品牌聚合
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg");
        brand_agg.field("brandId").size(50);
        //品牌之聚合
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        searchSourceBuilder.aggregation(brand_agg);
        //2、分类聚合 catalog_agg
        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        searchSourceBuilder.aggregation(catalog_agg);
        //3、属性聚合 attr_agg
        NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        //聚合当前所有的attrId
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
        //聚合当前attrId的名称
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        //聚合出当前attrId对应的所有的attrValue
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        attr_agg.subAggregation(attr_id_agg);
        searchSourceBuilder.aggregation(attr_agg);
        String s = searchSourceBuilder.toString();
        System.out.println("构建出来的dsl语句"+s);
        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, searchSourceBuilder);

        return searchRequest;
    }
}
