package com.yyh.gulimall.order.dao;

import com.yyh.gulimall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author yyh
 * @email 1697149091@qq.com
 * @date 2022-06-16 16:21:09
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
