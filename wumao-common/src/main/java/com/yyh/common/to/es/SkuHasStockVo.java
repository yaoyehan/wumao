package com.yyh.common.to.es;

import lombok.Data;

@Data
public class SkuHasStockVo {
    private Long skuId;
    private Boolean hasStock;
}
