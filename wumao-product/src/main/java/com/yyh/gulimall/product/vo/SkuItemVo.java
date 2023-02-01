package com.yyh.gulimall.product.vo;

import com.yyh.gulimall.product.entity.SkuImagesEntity;
import com.yyh.gulimall.product.entity.SkuInfoEntity;
import com.yyh.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
public class SkuItemVo {
    //1.sku基本信息获取 pms_sku_info
    SkuInfoEntity info;

    boolean hasStock =true;
    //2.sku的图片信息
    List<SkuImagesEntity> images;
    //3.获取sku的销售属性组合
    List<SpuItemSaleAttrsVo> saleAttrs;
    //4.获取sku的介绍
    SpuInfoDescEntity desp;
    //5.获取sku的规格参数信息
    List<SpuItemAttrGroupVo> groupAttrs;
    //6、秒杀商品的优惠信息
    private SeckillSkuVo seckillSkuVo;
    @ToString
    @Data
    public static class SpuItemSaleAttrsVo{
        private Long attrId;
        private String attrName;
        private List<AttrValueWithSkuIdVo> attrValues;
        @Data
        public static class  AttrValueWithSkuIdVo{
            private String attrValue;
            private String skuIds;

        }
    }
    @ToString
    @Data
    public static class SpuItemAttrGroupVo{
        private String groupName;
        private List<SpuBaseAttrVo> attrs ;
    }
    @Data
    public static class SpuBaseAttrVo{
        private String attrName;
        private String attrValue;
    }

}
