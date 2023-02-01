package com.yyh.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yyh.common.utils.PageUtils;
import com.yyh.gulimall.member.entity.GrowthChangeHistoryEntity;

import java.util.Map;

/**
 * 成长值变化历史记录
 *
 * @author yyh
 * @email 1697149091@qq.com
 * @date 2022-06-16 12:51:58
 */
public interface GrowthChangeHistoryService extends IService<GrowthChangeHistoryEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

