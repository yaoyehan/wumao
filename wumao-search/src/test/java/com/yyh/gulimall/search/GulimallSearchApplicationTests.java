package com.yyh.gulimall.search;

import com.alibaba.fastjson.JSON;
import com.yyh.gulimall.search.config.GulimallElasticSearchConfig;
import lombok.Data;
import lombok.ToString;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;

import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class GulimallSearchApplicationTests {
    @Autowired
    private RestHighLevelClient restHighLevelClient;



    @Test
    void searchData() throws IOException {
        SearchRequest request = new SearchRequest();
        request.indices("bank");
        //检索条件
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //按照年龄聚合
        searchSourceBuilder.query(QueryBuilders.matchQuery("address","mill"));
        TermsAggregationBuilder builder = AggregationBuilders.terms("ageAgg").field("age").size(10);
        searchSourceBuilder.aggregation(builder);
        AvgAggregationBuilder field = AggregationBuilders.avg("balanceAvg").field("balance");
        searchSourceBuilder.aggregation(field);
        request.source(searchSourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(request, GulimallElasticSearchConfig.COMMON_OPTIONS);
        System.out.println(searchResponse.toString());
        /*JSON.parseObject(searchResponse.toString(), Map.class);*/
        SearchHits hits = searchResponse.getHits();
        for (SearchHit hit : hits.getHits()) {
            String sourceAsString = hit.getSourceAsString();
            Account account = JSON.parseObject(sourceAsString, Account.class);
            System.out.println("account: "+account);
        }
        Aggregations aggregations = searchResponse.getAggregations();
/*        for (Aggregation aggregation : aggregations.asList()) {
            System.out.println("当前聚合"+aggregation.getName());
        }*/
        Terms ageAgg = aggregations.get("ageAgg");
        for (Terms.Bucket bucket : ageAgg.getBuckets()) {
            String keyAsString = bucket.getKeyAsString();
            System.out.println("年龄"+keyAsString+"==>"+bucket.getDocCount());
        }
        Avg balanceAvg = aggregations.get("balanceAvg");
        System.out.println("平均薪资"+balanceAvg.getValue());

    }




    @Test
    void indexData() throws IOException {
        IndexRequest request = new IndexRequest("users");
        request.id("1");
        User user = new User();
        user.setAge(20);
        String jsonString = JSON.toJSONString(user);
        request.source(jsonString, XContentType.JSON);
        IndexResponse index = restHighLevelClient.index(request, GulimallElasticSearchConfig.COMMON_OPTIONS);
        System.out.println(index);
    }
    @Data
    class User{
        private String userName;
        private String gender;
        private Integer age;

    }
    @ToString
    @Data
    static class Account
    {
        private int account_number;

        private int balance;

        private String firstname;

        private String lastname;

        private int age;

        private String gender;

        private String address;

        private String employer;

        private String email;

        private String city;

        private String state;

    }
    @Test
    void contextLoads() {
        System.out.println(restHighLevelClient);
    }

    @Test
    void threadTest(){
        Object o = new Object();
        o.toString();

    }
}
