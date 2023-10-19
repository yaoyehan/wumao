package com.yyh.gulimall.member.controller;

import java.util.Arrays;
import java.util.Map;


import com.yyh.common.exception.BizCodeEnume;
import com.yyh.gulimall.member.exception.MobileExistException;
import com.yyh.gulimall.member.exception.UsernameExistException;
import com.yyh.gulimall.member.feign.CouponFeignService;
import com.yyh.gulimall.member.vo.MemberLoginVo;
import com.yyh.gulimall.member.vo.MemberRegistVo;
import com.yyh.gulimall.member.vo.SocialUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.yyh.gulimall.member.entity.MemberEntity;
import com.yyh.gulimall.member.service.MemberService;
import com.yyh.common.utils.PageUtils;
import com.yyh.common.utils.R;



/**
 * 会员
 *
 * @author yyh
 * @email 1697149091@qq.com
 * @date 2022-06-16 12:51:58
 */
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;
    @Autowired
    CouponFeignService couponFeignService;
    @RequestMapping("/coupons")
    public R test(){
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setNickname("张三");
        R membercoupons = couponFeignService.membercoupons();
        return R.ok().put("member",memberEntity).put("coupons",membercoupons.get("coupons"));
    }
    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("member:member:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("member:member:info")
    public R info(@PathVariable("id") Long id){
		MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }


    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("member:member:save")
    public R save(@RequestBody MemberEntity member){
		memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("member:member:update")
    public R update(@RequestBody MemberEntity member){
		memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("member:member:delete")
    public R delete(@RequestBody Long[] ids){
		memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

    @PostMapping("login")
    public R login(@RequestBody MemberLoginVo vo){
        MemberEntity entity=memberService.login(vo);
        if(entity!=null){
            return R.ok();
        }else {
            return R.error(BizCodeEnume.LOGINACCOUNT_PASSWORD_INVAILD_EXCEPTION.getCode(),
                    BizCodeEnume.LOGINACCOUNT_PASSWORD_INVAILD_EXCEPTION.getMsg());
        }
    }

    @PostMapping("/regist")
    public R regist(@RequestBody MemberRegistVo vo){
        try {
            memberService.regist(vo);
        }catch (MobileExistException e){
            R.error(BizCodeEnume.PHONE_EXIT_EXCEPTION.getCode(),BizCodeEnume.PHONE_EXIT_EXCEPTION.getMsg());
        }catch (UsernameExistException e){
            R.error(BizCodeEnume.USER_EXIT_EXCEPTION.getCode(), BizCodeEnume.USER_EXIT_EXCEPTION.getMsg());
        }
        return R.ok();
    }
    @PostMapping("/oauth2/login")
    public R oauthlogin(@RequestBody SocialUser socialUser) throws Exception {
        MemberEntity user=memberService.login(socialUser);
        if(user!=null){
            return R.ok().setData(user);
        }else {
            return R.error(BizCodeEnume.LOGINACCOUNT_PASSWORD_INVAILD_EXCEPTION.getCode(),
                    BizCodeEnume.LOGINACCOUNT_PASSWORD_INVAILD_EXCEPTION.getMsg());
        }
    }


}