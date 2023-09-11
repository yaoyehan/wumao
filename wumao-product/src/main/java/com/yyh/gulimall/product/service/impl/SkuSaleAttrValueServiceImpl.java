package com.yyh.gulimall.product.service.impl;

import com.yyh.gulimall.product.vo.SkuItemVo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yyh.common.utils.PageUtils;
import com.yyh.common.utils.Query;

import com.yyh.gulimall.product.dao.SkuSaleAttrValueDao;
import com.yyh.gulimall.product.entity.SkuSaleAttrValueEntity;
import com.yyh.gulimall.product.service.SkuSaleAttrValueService;


@Service("skuSaleAttrValueService")
public class SkuSaleAttrValueServiceImpl extends ServiceImpl<SkuSaleAttrValueDao, SkuSaleAttrValueEntity> implements SkuSaleAttrValueService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuSaleAttrValueEntity> page = this.page(
                new Query<SkuSaleAttrValueEntity>().getPage(params),
                new QueryWrapper<SkuSaleAttrValueEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<SkuItemVo.SpuItemSaleAttrsVo> getSaleAttrsBySpuId(Long spuId) {
        List<SkuItemVo.SpuItemSaleAttrsVo> saleAttrsVos=this.baseMapper.getSaleAttrsBySpuId(spuId);
        return saleAttrsVos;
    }

    @Override
    public List<String> getSkuSaleAttrValuesAsStringList(Long skuId) {
        SkuSaleAttrValueDao dao=this.baseMapper;
        return dao.getSkuSaleAttrValuesAsStringList(skuId);
    }

}