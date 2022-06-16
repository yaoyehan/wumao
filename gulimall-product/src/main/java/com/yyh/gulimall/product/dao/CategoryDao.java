package com.yyh.gulimall.product.dao;

import com.yyh.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author yyh
 * @email 1697149091@qq.com
 * @date 2022-06-16 09:31:27
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
