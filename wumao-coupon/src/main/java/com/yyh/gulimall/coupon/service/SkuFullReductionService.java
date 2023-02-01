package com.yyh.gulimall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yyh.common.to.SkuReductionTo;
import com.yyh.common.utils.PageUtils;
import com.yyh.gulimall.coupon.entity.SkuFullReductionEntity;

import java.util.Map;

/**
 * 商品满减信息
 *
 * @author yyh
 * @email 1697149091@qq.com
 * @date 2022-06-16 11:55:02
 */
public interface SkuFullReductionService extends IService<SkuFullReductionEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSkuReduction(SkuReductionTo skuReductionTo);
}

