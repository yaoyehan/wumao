package com.yyh.gulimall.ware.service.impl;

import com.yyh.common.constant.WareConstant;
import com.yyh.common.utils.R;
import com.yyh.gulimall.ware.Vo.MergeVo;
import com.yyh.gulimall.ware.Vo.PurchaseDoneVo;
import com.yyh.gulimall.ware.Vo.PurchaseItemDoneVo;
import com.yyh.gulimall.ware.entity.PurchaseDetailEntity;
import com.yyh.gulimall.ware.service.PurchaseDetailService;
import com.yyh.gulimall.ware.service.WareSkuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yyh.common.utils.PageUtils;
import com.yyh.common.utils.Query;

import com.yyh.gulimall.ware.dao.PurchaseDao;
import com.yyh.gulimall.ware.entity.PurchaseEntity;
import com.yyh.gulimall.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {
    @Autowired
    PurchaseDetailService purchaseDetailService;
    @Autowired
    WareSkuService wareSkuService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageUnreceivePurchase(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>().eq("status",0).or().eq("status","1")
        );

        return new PageUtils(page);
    }
    @Transactional
    @Override
    public void mergePurchase(MergeVo mergeVo) {
        Long purchaseId = mergeVo.getPurchaseId();
        List<Long> items = mergeVo.getItems();
        //确认采购需求的状态是0/1才可以
        List<PurchaseDetailEntity> collect1 = items.stream().map(i -> {
            PurchaseDetailEntity byId = purchaseDetailService.getById(i);
            return byId;
        }).filter(item -> {
            if (item.getStatus() != WareConstant.PurchaseDetailStatusEnum.CREATED.getCode() ||
                    item.getStatus() != WareConstant.PurchaseStatusEnum.ASSIGNED.getCode()) {
                return true;
            }
            return false;
        }).collect(Collectors.toList());
        if(collect1.size()==0){
            //新建一个采购表
            if(purchaseId==null){
                PurchaseEntity purchaseEntity = new PurchaseEntity();
                purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATED.getCode());
                purchaseEntity.setCreateTime(new Date());
                purchaseEntity.setUpdateTime(new Date());
                this.save(purchaseEntity);
                purchaseId=purchaseEntity.getId();
            }

            //修改需求表
            Long finalPurchaseId=purchaseId;

            if(collect1.size()==0){
                List<PurchaseDetailEntity> collect = items.stream().map(i -> {
                    PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
                    purchaseDetailEntity.setId(i);
                    purchaseDetailEntity.setPurchaseId(finalPurchaseId);
                    purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());
                    return purchaseDetailEntity;
                }).collect(Collectors.toList());
                purchaseDetailService.updateBatchById(collect);
                PurchaseEntity purchaseEntity = new PurchaseEntity();
                purchaseEntity.setId(purchaseId);
                purchaseEntity.setUpdateTime(new Date());
                this.updateById(purchaseEntity);
            }
        }

    }

    @Override
    public void received(List<Long> ids) {
        //1、确认当前采购单是新建的还是已分配状态
        List<PurchaseEntity> collect = ids.stream().map(id -> {
            PurchaseEntity purchaseEntity = this.getById(id);
            return purchaseEntity;
        }).filter(item -> {
            if (item.getStatus() == WareConstant.PurchaseStatusEnum.CREATED.getCode() ||
                    item.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode()) {
                return true;
            } else {
                return false;
            }
        }).map(item -> {
            item.setStatus(WareConstant.PurchaseStatusEnum.RECEIVE.getCode());
            item.setUpdateTime(new Date());
            return item;
        }).collect(Collectors.toList());
        if(collect.size()!=0){
            //2、改变采购单的状态
            this.updateBatchById(collect);
        }

        //3、改变采购需求的状态
        for (PurchaseEntity item : collect) {
            List<PurchaseDetailEntity> detailEntityList = purchaseDetailService.listDetailByPurchaseId(item.getId());
            List<PurchaseDetailEntity> collect1 = detailEntityList.stream().map((item1) -> {
                PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
                purchaseDetailEntity.setId(item1.getId());
                purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.BUYING.getCode());
                return purchaseDetailEntity;
            }).collect(Collectors.toList());
            if (collect1.size()!=0){
                purchaseDetailService.updateBatchById(collect1);
            }

        }
    }

    @Override
    public void done(PurchaseDoneVo purchaseDoneVo) {

        Long id = purchaseDoneVo.getId();
        //2.改变需求项的状态
         Boolean flag=true;
        List<PurchaseItemDoneVo> items = purchaseDoneVo.getItems();
        ArrayList<PurchaseDetailEntity> list = new ArrayList<>();
        for (PurchaseItemDoneVo item : items) {
            PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
            if (item.getStatus() == WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode()||item.getStatus() == WareConstant.PurchaseDetailStatusEnum.FINISH.getCode()) {
                flag = false;
                purchaseDetailEntity.setStatus(item.getStatus());
            } else {
                purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.FINISH.getCode());
                //3.将成功采购的进行入库
                PurchaseDetailEntity entity = purchaseDetailService.getById(item.getItemId());
                wareSkuService.addStcok(entity.getSkuId(),entity.getWareId(),entity.getSkuNum());
            }
            purchaseDetailEntity.setId(item.getItemId());
            list.add(purchaseDetailEntity);
        }
        purchaseDetailService.updateBatchById(list);
        //1.改变采购单的状态
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(id);
        purchaseEntity.setStatus(flag?WareConstant.PurchaseStatusEnum.FINISH.getCode() :
                WareConstant.PurchaseStatusEnum.HASERROR.getCode());
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);


    }

}