package com.yyh.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yyh.common.utils.PageUtils;
import com.yyh.gulimall.order.entity.RefundInfoEntity;

import java.util.Map;

/**
 * 退款信息
 *
 * @author yyh
 * @email 1697149091@qq.com
 * @date 2022-06-16 16:21:10
 */
public interface RefundInfoService extends IService<RefundInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

