package com.yyh.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yyh.common.utils.PageUtils;
import com.yyh.gulimall.member.entity.MemberEntity;
import com.yyh.gulimall.member.exception.MobileExistException;
import com.yyh.gulimall.member.exception.UsernameExistException;
import com.yyh.gulimall.member.vo.MemberLoginVo;
import com.yyh.gulimall.member.vo.MemberRegistVo;
import com.yyh.gulimall.member.vo.SocialUser;

import java.awt.*;
import java.util.Map;

/**
 * 会员
 *
 * @author yyh
 * @email 1697149091@qq.com
 * @date 2022-06-16 12:51:58
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(MemberRegistVo vo);

    void checkMobileUnique(String mobile) throws MobileExistException;
    void checkUsernameUnique(String username) throws UsernameExistException;

    MemberEntity login(MemberLoginVo vo);

    MemberEntity login(SocialUser socialUser) throws Exception;
}

