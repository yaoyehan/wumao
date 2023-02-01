package com.yyh.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yyh.common.utils.HttpUtils;
import com.yyh.gulimall.member.dao.MemberLevelDao;
import com.yyh.gulimall.member.entity.MemberLevelEntity;
import com.yyh.gulimall.member.exception.MobileExistException;
import com.yyh.gulimall.member.exception.UsernameExistException;
import com.yyh.gulimall.member.vo.MemberLoginVo;
import com.yyh.gulimall.member.vo.MemberRegistVo;
import com.yyh.gulimall.member.vo.SocialUser;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yyh.common.utils.PageUtils;
import com.yyh.common.utils.Query;

import com.yyh.gulimall.member.dao.MemberDao;
import com.yyh.gulimall.member.entity.MemberEntity;
import com.yyh.gulimall.member.service.MemberService;
import org.springframework.transaction.annotation.Transactional;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {
    @Autowired
    MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }
    @Transactional
    @Override
    public void regist(MemberRegistVo vo) {
        MemberEntity memberEntity = new MemberEntity();
        //设置默认等级
        MemberLevelEntity memberLevelEntity= memberLevelDao.getDefaultLevel();
        memberEntity.setLevelId(memberLevelEntity.getId());
        //检查用户名和手机号是否唯一
        checkMobileUnique(vo.getPhone());
        checkUsernameUnique(vo.getUsername());
        memberEntity.setMobile(vo.getPhone());
        memberEntity.setUsername(vo.getUsername());
        memberEntity.setNickname(vo.getUsername());
        //密码加密储存
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode(vo.getPassword());
        memberEntity.setPassword(encode);
        baseMapper.insert(memberEntity);
    }

    @Override
    public void checkMobileUnique(String mobile) throws MobileExistException{
        Integer count = baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", mobile));
        if(count>0){
            throw new MobileExistException();
        }

    }

    @Override
    public void checkUsernameUnique(String username) throws UsernameExistException{
        Integer count = baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", username));
        if(count>0){
            throw new UsernameExistException();
        }
    }

    @Override
    public MemberEntity login(MemberLoginVo vo) {
        String loginAccount=vo.getLoginAccount();
        String password=vo.getPassword();
        //1.去数据库查询
        MemberDao memberDao=this.baseMapper;
        MemberEntity entity = memberDao.selectOne(new QueryWrapper<MemberEntity>().
                eq("username", loginAccount).or().eq("mobile", loginAccount));
        if(entity==null){
            return null;
        }else {
            //获取数据库的password
            String passwordDb = entity.getPassword();
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            //密码匹配
            boolean matches = passwordEncoder.matches(password, passwordDb);
            if(matches){
                return entity;
            }else {
                return null;
            }
        }
    }
    @Transactional
    @Override
    public MemberEntity login(SocialUser socialUser) throws Exception {
        HashMap<String, String> headers = new HashMap<>();
        Map<String, String> query = new HashMap<>();
        query.put("access_token", socialUser.getAccessToken());
        HttpResponse response = HttpUtils.doGet("https://gitee.com", "/api/v5/user", "get", headers, query);
        try {
            if (response.getStatusLine().getStatusCode() == 200) {
                String json = EntityUtils.toString(response.getEntity());
                JSONObject jsonObject = JSON.parseObject(json);
                socialUser.setSocialUid(jsonObject.get("id") + "");
                //登录和注册合并逻辑
                String uid = socialUser.getSocialUid();
                MemberEntity memberEntity = baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", uid));
                if (memberEntity != null) {
                    //这个用户已经注册
                    MemberEntity update = new MemberEntity();
                    update.setAccessToken(socialUser.getAccessToken());
                    update.setExpiresIn(socialUser.getExpiresIn());
                    baseMapper.updateById(update);
                    memberEntity.setAccessToken(socialUser.getAccessToken());
                    memberEntity.setExpiresIn(socialUser.getExpiresIn());
                    return memberEntity;
                } else {
                    //2.没有查到用户就需要注册一个
                    MemberEntity regist = new MemberEntity();
                    //获得用户名字
                    String name = jsonObject.getString("name");
                    regist.setNickname(name);
                    regist.setSocialUid(uid);
                    regist.setAccessToken(socialUser.getAccessToken());
                    regist.setExpiresIn(socialUser.getExpiresIn());
                    baseMapper.insert(regist);
                    return regist;
                }
            }
        } catch (Exception e) {
        }
        return null;
    }


}