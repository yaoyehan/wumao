package com.yyh.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.impl.AMQImpl;
import com.yyh.common.exception.NoStockException;
import com.yyh.common.to.OrderTo;
import com.yyh.common.to.mq.StockDetailTo;
import com.yyh.common.to.mq.StockLockedTo;
import com.yyh.common.utils.R;
import com.yyh.gulimall.ware.Vo.OrderItemVo;
import com.yyh.gulimall.ware.Vo.OrderVo;
import com.yyh.gulimall.ware.Vo.SkuHasStockVo;
import com.yyh.gulimall.ware.Vo.WareSkuLockVo;
import com.yyh.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.yyh.gulimall.ware.entity.WareOrderTaskEntity;
import com.yyh.gulimall.ware.feign.OrderFeignService;
import com.yyh.gulimall.ware.feign.ProductFeignService;
import com.yyh.gulimall.ware.service.WareOrderTaskDetailService;
import com.yyh.gulimall.ware.service.WareOrderTaskService;
import lombok.Data;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yyh.common.utils.PageUtils;
import com.yyh.common.utils.Query;

import com.yyh.gulimall.ware.dao.WareSkuDao;
import com.yyh.gulimall.ware.entity.WareSkuEntity;
import com.yyh.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import sun.text.resources.cldr.ar.FormatData_ar_QA;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {
    @Autowired
    WareSkuDao wareSkuDao;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    WareOrderTaskDetailService wareOrderTaskDetailService;
    @Autowired
    WareOrderTaskService wareOrderTaskService;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    OrderFeignService orderFeignService;


    /**
     * 解锁库存的方法
     * @param skuId
     * @param wareId
     * @param num
     * @param taskDetailId
     */
    public void unLockStock(Long skuId,Long wareId,Integer num,Long taskDetailId) {

        //库存解锁
        wareSkuDao.unLockStock(skuId,wareId,num);
        //更新工作单的状态
        WareOrderTaskDetailEntity taskDetailEntity = new WareOrderTaskDetailEntity();
        taskDetailEntity.setId(taskDetailId);
        //变为已解锁
        taskDetailEntity.setLockStatus(2);
        wareOrderTaskDetailService.updateById(taskDetailEntity);

    }


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageUtils(page);
    }
    @Transactional
    @Override
    public void addStcok(Long skuId, Long wareId, Integer skuNum) {
        List<WareSkuEntity> list = this.list(new QueryWrapper<WareSkuEntity>().eq("ware_id", wareId).eq("sku_id", skuId));
        if(list.size()==0||list==null){
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setStockLocked(0);
            //远程查询sku名称
            try {
                R info = productFeignService.info(skuId);
                Map<String,Object> map= (Map<String, Object>) info.get("skuInfo");
                if(info.getCode()==0){
                    wareSkuEntity.setSkuName((String) map.get("skuName"));
                }
            }catch (Exception e){
            }
            wareSkuDao.insert(wareSkuEntity);
        }else {
            wareSkuDao.addStock(skuId,wareId,skuNum);
        }

    }

    @Override
    public List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds) {
        List<SkuHasStockVo> collect = skuIds.stream().map(skuId -> {
            SkuHasStockVo skuHasStockVo = new SkuHasStockVo();
            Long count = baseMapper.getSkuStock(skuId);
            skuHasStockVo.setSkuId(skuId);
            skuHasStockVo.setHasStock(count == null ? false : count > 0);
            return skuHasStockVo;
        }).collect(Collectors.toList());
        return collect;

    }

    /**
     * 为某个订单锁定库存
     * @param wareSkuLockVo
     * @return
     */
    @Transactional
    @Override
    public Boolean orderLockStock(WareSkuLockVo wareSkuLockVo) {
        /**
         * 保存库存工作单详情信息
         * 追溯
         */
        WareOrderTaskEntity wareOrderTaskEntity = new WareOrderTaskEntity();
        wareOrderTaskEntity.setOrderSn(wareSkuLockVo.getOrderSn());
        wareOrderTaskEntity.setCreateTime(new Date());
        wareOrderTaskService.save(wareOrderTaskEntity);
        //1、按照下单的收货地址，找到一个就近仓库，锁定库存
        //2、找到每个商品在哪个仓库都有库存
        List<OrderItemVo> locks = wareSkuLockVo.getLocks();

        List<SkuWareHasStock> collect = locks.stream().map((item) -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            stock.setNum(item.getCount());
            //查询这个商品在哪个仓库有库存
            List<Long> wareIdList = wareSkuDao.listWareIdHasSkuStock(skuId);
            stock.setWareId(wareIdList);
            return stock;
        }).collect(Collectors.toList());
        //2.锁定库存
        for (SkuWareHasStock hasStock : collect) {
            Boolean skuStocked=false;
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareId();
            if(wareIds==null||wareIds.size()==0){
                //没有任何仓库有商品库存
                throw new NoStockException(skuId);
            }
            for (Long wareId : wareIds) {
                //成功就是1，不成功就是0
                Long count=wareSkuDao.lockSkuStock(skuId,wareId,hasStock.getNum());
                if(count==1){
                    skuStocked = true;
                    //给数据库保存库存锁定的详细信息
                    WareOrderTaskDetailEntity taskDetailEntity = WareOrderTaskDetailEntity.builder()
                            .skuId(skuId)
                            .skuName("")
                            .skuNum(hasStock.getNum())
                            .taskId(wareOrderTaskEntity.getId())
                            .wareId(wareId)
                            .lockStatus(1)
                            .build();
                    wareOrderTaskDetailService.save(taskDetailEntity);
                    //TODO 告诉MQ库存锁定成功
                    StockLockedTo lockedTo = new StockLockedTo();
                    lockedTo.setId(wareOrderTaskEntity.getId());
                    StockDetailTo detailTo = new StockDetailTo();
                    BeanUtils.copyProperties(taskDetailEntity,detailTo);
                    lockedTo.setDetailTo(detailTo);
                    rabbitTemplate.convertAndSend("stock-event-exchange","stock.locked",lockedTo);
                }else {
                    //当前仓库锁失败，重试下一个仓库
                }
            }
            if (skuStocked == false) {
                //当前商品所有仓库都没有锁住
                throw new NoStockException(skuId);
            }
        }
        return true;
    }

    @Override
    public void unlockStock(StockLockedTo to) {

            StockDetailTo detailTo = to.getDetailTo();
            Long detailToId = detailTo.getId();

            WareOrderTaskDetailEntity byId = wareOrderTaskDetailService.getById(detailToId);
            if(byId!=null){
                //解锁
                Long id = to.getId();
                WareOrderTaskEntity taskEntity = wareOrderTaskService.getById(id);
                String orderSn = taskEntity.getOrderSn();
                R r = orderFeignService.getOrderStatus(orderSn);
                if(r.getCode()==0){
                    OrderVo data = r.getData(new TypeReference<OrderVo>() {
                    });
                    if(data==null||data.getStatus()==4){
                        if(byId.getLockStatus()==1){
                            //订单被取消，解锁库存
                            unLockStock(detailTo.getSkuId(),detailTo.getWareId(),detailTo.getSkuNum(),detailToId);
                        }
                    }
                }else {
                    throw new RuntimeException("远程服务失败");
                }
            }

    }
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void unlockStock(OrderTo orderTo) {

        String orderSn = orderTo.getOrderSn();
        //查一下最新的库存解锁状态，防止重复解锁库存
        WareOrderTaskEntity orderTaskEntity = wareOrderTaskService.getOrderTaskByOrderSn(orderSn);

        //按照工作单的id找到所有 没有解锁的库存，进行解锁
        Long id = orderTaskEntity.getId();
        List<WareOrderTaskDetailEntity> list = wareOrderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>()
                .eq("task_id", id).eq("lock_status", 1));

        for (WareOrderTaskDetailEntity taskDetailEntity : list) {
            unLockStock(taskDetailEntity.getSkuId(),
                    taskDetailEntity.getWareId(),
                    taskDetailEntity.getSkuNum(),
                    taskDetailEntity.getId());
        }


    }


    @Data
    class SkuWareHasStock {
        private Long skuId;
        private Integer num;
        private List<Long> wareId;
    }
}