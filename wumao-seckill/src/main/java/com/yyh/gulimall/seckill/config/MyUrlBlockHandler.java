package com.yyh.gulimall.seckill.config;



import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.yyh.common.exception.BizCodeEnume;
import com.yyh.common.utils.R;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class MyUrlBlockHandler implements BlockExceptionHandler {



    @Override
    public void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, BlockException e) throws Exception {
        R error = R.error(BizCodeEnume.TOO_MANY_REQUEST.getCode(), BizCodeEnume.TOO_MANY_REQUEST.getMsg());
        httpServletResponse.setCharacterEncoding("UTF-8");
        httpServletResponse.setContentType("application/json");
        httpServletResponse.getWriter().write(JSON.toJSONString(error));
    }
}
