package com.yyh.gulimall.product.vo;

import com.yyh.gulimall.product.entity.CategoryEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Catalog2Vo {
    private String catalog1Id;
    private List<Catalog3Vo> catalog3List;
    private String id;
    private String name;
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Catalog3Vo{
        private String catalog2Id;
        private String id;
        private String name;

    }
}
