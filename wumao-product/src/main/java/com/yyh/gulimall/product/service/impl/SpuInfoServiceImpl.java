package com.yyh.gulimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.yyh.common.constant.ProductConstant;
import com.yyh.common.to.es.SkuEsModel;
import com.yyh.common.to.SkuReductionTo;
import com.yyh.common.to.SpuBoundTo;
import com.yyh.common.to.es.SkuHasStockVo;
import com.yyh.common.utils.R;
import com.yyh.gulimall.product.entity.*;
import com.yyh.gulimall.product.feign.CouponFeignService;
import com.yyh.gulimall.product.feign.SearchFeignService;
import com.yyh.gulimall.product.feign.WareFeignService;
import com.yyh.gulimall.product.service.*;
import com.yyh.gulimall.product.vo.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yyh.common.utils.PageUtils;
import com.yyh.common.utils.Query;

import com.yyh.gulimall.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {
    @Autowired
    SpuInfoDescService spuInfoDescService;
    @Autowired
    SpuImagesService spuImagesService;
    @Autowired
    AttrService attrService;
    @Autowired
    ProductAttrValueService attrValueService;
    @Autowired
    SearchFeignService searchFeignService;
    @Autowired
    SkuInfoService skuInfoService;
    @Autowired
    SkuImagesService skuImagesService;
    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;
    @Autowired
    CouponFeignService couponFeignService;
    @Autowired
    BrandService brandService;
    @Autowired
    CategoryService categoryService;
    @Autowired
    WareFeignService wareFeignService;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }
    //TODO 高级篇完善
    @Transactional
    @Override
    public void saveSupInfo(SpuSaveVo vo) {
        //1.保存spu的基本信息 `pms_spu_info`
        SpuInfoEntity infoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(vo,infoEntity);
        infoEntity.setCreateTime(new Date());
        infoEntity.setUpdateTime(new Date());
        this.saveBaseSpuInfo(infoEntity);

        //2。保存Spu的描述图片 `pms_spu_info_desc`
        List<String> decript = vo.getDecript();
        SpuInfoDescEntity spuInfoDescEntity = new SpuInfoDescEntity();
        spuInfoDescEntity.setSpuId(infoEntity.getId());
        spuInfoDescEntity.setDecript(String.join(",",decript));
        spuInfoDescService.saveSpuInfoDesc(spuInfoDescEntity);
        //TODO 没有图片路径无需保存
        //3.保存Spu的图片集 `pms_spu_images`
        List<String> images = vo.getImages();
        spuImagesService.saveImages(infoEntity.getId(),images);
        //4.保存Spu的规格参数 `pms_product_attr_value`
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
        List<ProductAttrValueEntity> collect = baseAttrs.stream().map(item -> {
            ProductAttrValueEntity productAttrValueEntity = new ProductAttrValueEntity();
            productAttrValueEntity.setAttrId(item.getAttrId());
            productAttrValueEntity.setAttrValue(item.getAttrValues());
            productAttrValueEntity.setSpuId(infoEntity.getId());
            AttrEntity attrEntity = attrService.getById(item.getAttrId());
            productAttrValueEntity.setAttrName(attrEntity.getAttrName());
            productAttrValueEntity.setQuickShow(item.getShowDesc());
            return productAttrValueEntity;
        }).collect(Collectors.toList());
        attrValueService.saveProductAttr(collect);
        //5、保存spu的积分信息 `gulimall_sms`->`sms_spu_bounds`
        Bounds bounds = vo.getBounds();
        SpuBoundTo spuBoundTo = new SpuBoundTo();
        BeanUtils.copyProperties(bounds,spuBoundTo);
        spuBoundTo.setSpuId(infoEntity.getId());
        //跨服务调用保存
        R r = couponFeignService.saveSpuBounds(spuBoundTo);
        if(r.getCode()!=0){
            log.error("远程保存积分信息失败");
        }
        //6、保存当前spu对应的sku的基本信息

        List<Skus> skus = vo.getSkus();
        if(skus!=null&& skus.size()>0){
            skus.forEach(item->{
                String DefaultImg="";
                for (Images image : item.getImages()) {
                    if(image.getDefaultImg()==1){
                        DefaultImg=image.getImgUrl();
                    }
                }
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(item,skuInfoEntity);
                skuInfoEntity.setBrandId(infoEntity.getBrandId());
                skuInfoEntity.setCatalogId(infoEntity.getCatalogId());
                skuInfoEntity.setSaleCount(0L);
                skuInfoEntity.setSpuId(infoEntity.getId());
                skuInfoEntity.setSkuDefaultImg(DefaultImg);
                //6.1、sku的基本信息 `pms_sku_info`
                skuInfoService.saveSkuInfo(skuInfoEntity);
                Long skuId = skuInfoEntity.getSkuId();
                List<SkuImagesEntity> imagesEntities = item.getImages().stream().map(img -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setImgUrl(img.getImgUrl());
                    skuImagesEntity.setDefaultImg(img.getDefaultImg());
                    return skuImagesEntity;
                }).filter(entity->{
                    return !StringUtils.isEmpty(entity.getImgUrl());
                }).collect(Collectors.toList());
                //6.2、sku的图片信息 `pms_spu_images`
                skuImagesService.saveBatch(imagesEntities);
                List<Attr> attr = item.getAttr();
                List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = attr.stream().map(a -> {
                    SkuSaleAttrValueEntity skuSaleAttrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(a, skuSaleAttrValueEntity);
                    skuSaleAttrValueEntity.setSkuId(skuId);
                    return skuSaleAttrValueEntity;
                }).collect(Collectors.toList());
                //6.3、sku的销售信息 `pms_sku_sale_attr_value`
                skuSaleAttrValueService.saveBatch(skuSaleAttrValueEntities);
                //6.4、sku的优惠满减信息 `gulimall_sms`-> `sms_sku_ladder`\`sms_sku_full_reduction`
                SkuReductionTo skuReductionTo = new SkuReductionTo();
                //把memberPrice单独拿出来
                List<MemberPrice> memberPrice = item.getMemberPrice();
                List<com.yyh.common.to.MemberPrice> collect1 = memberPrice.stream().map(mp -> {
                    com.yyh.common.to.MemberPrice memberPrice1 = new com.yyh.common.to.MemberPrice();
                    memberPrice1.setId(mp.getId());
                    memberPrice1.setPrice(mp.getPrice());
                    memberPrice1.setName(mp.getName());
                    return memberPrice1;
                }).collect(Collectors.toList());
                BeanUtils.copyProperties(item,skuReductionTo);
                skuReductionTo.setMemberPrice(collect1);
                skuReductionTo.setSkuId(skuId);
                if(skuReductionTo.getFullCount()>0||skuReductionTo.getFullPrice().compareTo(new BigDecimal("0"))==1){
                    R r1 = couponFeignService.saveSkuReduction(skuReductionTo);
                    if(r1.getCode()!=0){
                        log.error("远程保存sku优惠信息失败");
                    }
                }

            });
        }


    }

    @Override
    public void saveBaseSpuInfo(SpuInfoEntity infoEntity) {
        baseMapper.insert(infoEntity);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();
        String key =(String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            wrapper.
                eq("id",key).or().like("spu_name",key);

        }
        String status =(String) params.get("status");
        if(!StringUtils.isEmpty(status)){
            wrapper.
                eq("publish_status",status);

        }
        String brandId =(String) params.get("brandId");
        if(!StringUtils.isEmpty(brandId)&&!"0".equalsIgnoreCase(brandId)){
            wrapper.
                eq("brand_id",brandId);

        }
        String catalogId =(String) params.get("catalogId");
        if(!StringUtils.isEmpty(catalogId)&&!"0".equalsIgnoreCase(catalogId)){
            wrapper.
                eq("catalog_id",catalogId);

        }

        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                wrapper
        );
        return new PageUtils(page);
    }

    @Override
    public void up(Long spuId) {
        //查询当前所有sku的规格属性
        List<ProductAttrValueEntity> baseAttrlistforspu = attrValueService.baseAttrlistforspu(spuId);
        List<Long> attrIds = baseAttrlistforspu.stream().map(item -> {
            return item.getAttrId();
        }).collect(Collectors.toList());
        List<Long> selectSearchAttrIds =attrValueService.selectSearchAttrIds(attrIds);
        HashSet<Long> setAttrIds = new HashSet<>(selectSearchAttrIds);
        List<SkuEsModel.Attr> attrList = baseAttrlistforspu.stream().filter(item -> {
            return setAttrIds.contains(item.getAttrId());
        }).map(item -> {
            SkuEsModel.Attr attr = new SkuEsModel.Attr();
            BeanUtils.copyProperties(item, attr);
            return attr;
        }).collect(Collectors.toList());
        //1、查出当前spuid对应的所有sku信息，品牌名字
        List<SkuInfoEntity> skuInfoEntities=skuInfoService.getSkuBySpuId(spuId);
        List<Long> skuIdList = skuInfoEntities.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());
        //TODO 1、发送远程调用服务查询库存
        Map<Long, Boolean> stockMap=null;
        try {
            R skuHasStock = wareFeignService.getSkuHasStock(skuIdList);
            TypeReference<List<SkuHasStockVo>> typeReference = new TypeReference<List<SkuHasStockVo>>(){};
            stockMap= skuHasStock.getData(typeReference).stream().collect(Collectors.toMap(SkuHasStockVo::getSkuId, item -> item.getHasStock()));
        }catch (Exception e){
            log.error("库存服务查询异常，原因{}",e);
        }
        //2、封装每个sku的信息
        Map<Long, Boolean> finalStockMap = stockMap;
        List<SkuEsModel> upProduct = skuInfoEntities.stream().map(sku -> {
            //组装数据
            SkuEsModel skuEsModel = new SkuEsModel();
            BeanUtils.copyProperties(sku, skuEsModel);
            //skuPrice,skuImg
            skuEsModel.setSkuPrice(sku.getPrice());
            skuEsModel.setSkuImg(sku.getSkuDefaultImg());
            //hasStock,hotScore
            //是否有库存
            if (finalStockMap == null) {
                skuEsModel.setHasStock(true);
            } else {
                skuEsModel.setHasStock(finalStockMap.get(sku.getSkuId()));
            }
            //TODO 2、热度评分
            skuEsModel.setHotScore(0L);
            //查询品牌和分类名字
            BrandEntity brandEntity = brandService.getById(sku.getBrandId());
            skuEsModel.setBrandName(brandEntity.getName());
            skuEsModel.setBrandImg(brandEntity.getLogo());
            CategoryEntity categoryEntity = categoryService.getById(sku.getCatalogId());
            skuEsModel.setCatalogName(categoryEntity.getName());
            skuEsModel.setCatalogId(sku.getCatalogId());
            skuEsModel.setAttrs(attrList);
            return skuEsModel;
        }).collect(Collectors.toList());
        //将包装好的上架商品信息进行保存：gulimall-search；
        R r = searchFeignService.productStatusUp(upProduct);
        if(r.getCode()==0){
            //远程调用成功
            //修改当前spu的状态为已上架
            baseMapper.updateSpuStatus(spuId, ProductConstant.StatusEnum.SPU_UP.getCode());
        }else {
            //远程调用失败
        }

    }

    @Override
    public SpuInfoEntity getSpuInfoBySkuId(Long skuId) {
        SkuInfoEntity skuInfoEntity = skuInfoService.getById(skuId);
        Long spuId = skuInfoEntity.getSpuId();
        SpuInfoEntity spuInfoEntity = getById(spuId);
        return spuInfoEntity;
    }

}