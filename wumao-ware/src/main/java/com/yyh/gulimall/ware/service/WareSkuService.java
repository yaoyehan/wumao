package com.yyh.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yyh.common.to.OrderTo;
import com.yyh.common.to.mq.StockLockedTo;
import com.yyh.common.utils.PageUtils;
import com.yyh.gulimall.ware.Vo.LockStockResultVo;
import com.yyh.gulimall.ware.Vo.SkuHasStockVo;
import com.yyh.gulimall.ware.Vo.WareSkuLockVo;
import com.yyh.gulimall.ware.entity.WareSkuEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author yyh
 * @email 1697149091@qq.com
 * @date 2022-06-16 17:05:54
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStcok(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds);

    Boolean orderLockStock(WareSkuLockVo wareSkuLockVo);

    void unlockStock(StockLockedTo to);

    void unlockStock(OrderTo to);
}

