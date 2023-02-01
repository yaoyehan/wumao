package com.yyh.gulimall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yyh.common.utils.PageUtils;
import com.yyh.gulimall.coupon.entity.HomeSubjectSpuEntity;

import java.util.Map;

/**
 * 专题商品
 *
 * @author yyh
 * @email 1697149091@qq.com
 * @date 2022-06-16 11:55:02
 */
public interface HomeSubjectSpuService extends IService<HomeSubjectSpuEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

