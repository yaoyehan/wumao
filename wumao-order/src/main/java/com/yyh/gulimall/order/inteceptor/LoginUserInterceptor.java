package com.yyh.gulimall.order.inteceptor;

import com.yyh.common.constant.AuthServerConstant;
import com.yyh.common.vo.MemberRespVo;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

@Component
public class LoginUserInterceptor implements HandlerInterceptor {
    public static ThreadLocal<MemberRespVo> loginUser=new ThreadLocal<MemberRespVo>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        boolean match = new AntPathMatcher().match("/order/order/status/**", uri);
        boolean match1 = new AntPathMatcher().match("/payed/notify", uri);
        if(match||match1){
            return true;
        }
        MemberRespVo data = (MemberRespVo) request.getSession().getAttribute(AuthServerConstant.LOGIN_USER);
        if(data!=null){
            loginUser.set(data);
            return true;
        }else {
            request.getSession().setAttribute("msg","请先登录");
            response.sendRedirect("http://gulimall.com/login.html");
            return false;
        }
    }
}
