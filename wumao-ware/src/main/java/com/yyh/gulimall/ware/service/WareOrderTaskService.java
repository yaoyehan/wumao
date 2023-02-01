package com.yyh.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yyh.common.utils.PageUtils;
import com.yyh.gulimall.ware.entity.WareOrderTaskEntity;

import java.util.Map;

/**
 * 库存工作单
 *
 * @author yyh
 * @email 1697149091@qq.com
 * @date 2022-06-16 17:05:54
 */
public interface WareOrderTaskService extends IService<WareOrderTaskEntity> {

    PageUtils queryPage(Map<String, Object> params);

    WareOrderTaskEntity getOrderTaskByOrderSn(String orderSn);
}

