package com.yyh.gulimall.cart.interceptor;

import com.yyh.common.constant.AuthServerConstant;
import com.yyh.common.constant.CartConstant;
import com.yyh.common.vo.MemberRespVo;
import com.yyh.gulimall.cart.vo.UserInfoTo;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

/**
 * 在执行目标方法之前，判断用户的登录状态，并封装传递给controller目标请求
 */
@Component
public class CartInterceptor implements HandlerInterceptor {
    public static ThreadLocal<UserInfoTo> threadLocal=new ThreadLocal<>();
    /**
     * 目标方法执行之前拦截
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserInfoTo userInfoTo = new UserInfoTo();
        HttpSession session=request.getSession();

        MemberRespVo member = (MemberRespVo)session.getAttribute(AuthServerConstant.LOGIN_USER);
        if(member!=null){
            //用户登录
            userInfoTo.setUserId(member.getId());
        }
        Cookie[] cookies = request.getCookies();
        if(cookies!=null){
            for (Cookie cookie : cookies) {
                String name = cookie.getName();
                if(name.equals(CartConstant.TEMP_USER_COOKIE_NAME)){
                    userInfoTo.setUserkey(cookie.getValue());
                    userInfoTo.setTempUser(true);
                }
            }
        }
        //如果用户没有userkey，分配一个
        if(StringUtils.isEmpty(userInfoTo.getUserkey())){
            String uuid= UUID.randomUUID().toString().replace("-","");
            userInfoTo.setUserkey(uuid);
        }
        threadLocal.set(userInfoTo);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        UserInfoTo userInfoTo = threadLocal.get();
        if(!userInfoTo.isTempUser()){
            Cookie cookie = new Cookie(CartConstant.TEMP_USER_COOKIE_NAME, userInfoTo.getUserkey());
            cookie.setMaxAge(CartConstant.TEMP_USER_COOKIE_TIMEOUT);
            cookie.setDomain("gulimall.com");
            response.addCookie(cookie);
        }
    }
}
