package com.yyh.gulimall.authserver.controller;

import com.alibaba.fastjson.TypeReference;
import com.alibaba.nacos.common.utils.UuidUtils;
import com.yyh.common.constant.AuthServerConstant;
import com.yyh.common.exception.BizCodeEnume;
import com.yyh.common.utils.R;
import com.yyh.common.vo.MemberRespVo;
import com.yyh.gulimall.authserver.feign.MemberFeignService;
import com.yyh.gulimall.authserver.feign.ThirdPartFeignService;
import com.yyh.gulimall.authserver.vo.UserLoginVo;
import com.yyh.gulimall.authserver.vo.UserRegistVo;
import org.apache.http.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.websocket.Session;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class LoginController {
    @Autowired
    ThirdPartFeignService thirdPartFeignService;
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    MemberFeignService memberFeignService;
    @ResponseBody
    @GetMapping("/sms/sendCode")
    public R sendCode(@RequestParam("phone") String phone){
        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if(!StringUtils.isEmpty(redisCode)){
            Long l=Long.parseLong(redisCode.split("_")[1]);
            if(System.currentTimeMillis()-l<60000){
                //60秒内不能再发
                return R.error(BizCodeEnume.SMS_CODE_EXCEPTION.getMsg());
            }
        }

        String code = UUID.randomUUID().toString().substring(0, 5);
        String rCode=code+"_"+System.currentTimeMillis();
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX+phone,rCode,10, TimeUnit.MINUTES);
        thirdPartFeignService.sendCode(phone,code);
        return R.ok();
    }

    @PostMapping("/regist")
    public String regist(@Valid UserRegistVo vo, BindingResult result, RedirectAttributes redirectAttributes){
        if(result.hasErrors()){
            //检验出错
/*            result.getFieldErrors().stream().map(fieldError -> {
                String field=fieldError.getField();
                String defaultMessage = fieldError.getDefaultMessage();
                errors.put(field,defaultMessage);
            })*/
            HashMap<String, String> errors=
                    (HashMap<String, String>) result.getFieldErrors().stream().
                            collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://auth.gulimall.com/reg.html";
        }
        //1、校验验证码
        String code = vo.getCode();
        String s = redisTemplate.opsForValue().get(
                AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if(!StringUtils.isEmpty(s)){
            //验证码校验通过
            if(code.equals(s.split("_")[0])){
                redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX+vo.getPhone());
                R regist = memberFeignService.regist(vo);
                if(regist.getCode()==0){
                    //成功
                    return "redirect:http://auth.gulimall.com/login.html";
                }else {
                    HashMap<String, String> errors=new HashMap<>();
                    errors.put("msg",regist.get("msg").toString());
                    redirectAttributes.addFlashAttribute("errors",errors);
                    return "redirect:http://auth.gulimall.com/reg.html";
                }
            }else {
                //验证码校验失败
                HashMap<String, String> errors = new HashMap<>();
                errors.put("code","验证码错误");
                redirectAttributes.addFlashAttribute("errors",errors);
                return "redirect:http://auth.gulimall.com/reg.html";
            }

        }else {
            HashMap<String, String> errors = new HashMap<>();
            errors.put("code","验证码错误");
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://auth.gulimall.com/reg.html";
        }
    }

    @GetMapping("/login.html")
    public String loginPage(HttpSession session){
        Object attribute = session.getAttribute(AuthServerConstant.LOGIN_USER);
        if(attribute==null){
            //没登录
            return "login";
        }else {
            return "redirect:http://gulimall.com";
        }
    }
    @PostMapping("/login")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String login(UserLoginVo vo, RedirectAttributes redirectAttributes, HttpSession session){
        R login = memberFeignService.login(vo);
        if(login.getCode()==0){
            //成功
            MemberRespVo data = login.getData("data", new TypeReference<MemberRespVo>() {
            });
            session.setAttribute(AuthServerConstant.LOGIN_USER,data);
            //远程登录
            return "redirect:http://gulimall.com";
        }else {
            HashMap<String, String> errors = new HashMap<>();
            errors.put("msg",login.getData("msg",new TypeReference<String>(){}));
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }

}
