package com.yyh.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yyh.common.utils.PageUtils;
import com.yyh.gulimall.product.entity.CategoryEntity;
import com.yyh.gulimall.product.vo.Catalog2Vo;

import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author yyh
 * @email 1697149091@qq.com
 * @date 2022-06-19 10:13:21
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<CategoryEntity> listWithTree();

    void removeMenuByIds(List<Long> asList);

    Long[] findcatalogPath(Long catalogId);

    void updateCascade(CategoryEntity category);

    Map<String, List<Catalog2Vo>> getCatalogJson();

    List<CategoryEntity> getLevel1Categorys();




}

