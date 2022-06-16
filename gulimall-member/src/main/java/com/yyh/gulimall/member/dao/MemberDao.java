package com.yyh.gulimall.member.dao;

import com.yyh.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author yyh
 * @email 1697149091@qq.com
 * @date 2022-06-16 12:51:58
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
