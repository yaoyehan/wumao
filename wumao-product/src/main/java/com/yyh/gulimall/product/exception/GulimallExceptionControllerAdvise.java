package com.yyh.gulimall.product.exception;

import com.yyh.common.exception.BizCodeEnume;
import com.yyh.common.utils.R;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
/*@ResponseBody
@ControllerAdvice(basePackages = "com.yyh.gulimall.product.app")*/
@RestControllerAdvice(basePackages = "com.yyh.gulimall.product.app")
public class GulimallExceptionControllerAdvise {

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public R handleValidException(MethodArgumentNotValidException exception){
        log.error("数据校验出现问题{},异常类型：{}", exception.getMessage(),exception.getClass());
        BindingResult bindingResult = exception.getBindingResult();
        HashMap<String, String> errorsMap = new HashMap<>();
        bindingResult.getFieldErrors().forEach((fieldErrors->{
            errorsMap.put(fieldErrors.getField(),fieldErrors.getDefaultMessage());
        }));
        return R.error(BizCodeEnume.VAILD_EXCEPTION.getCode(), BizCodeEnume.VAILD_EXCEPTION.getMsg()).put("data",errorsMap);
    }
    @ExceptionHandler(value = Throwable.class)
    public R handleException(Throwable throwable){
        log.error("错误:",throwable);
        return R.error(BizCodeEnume.UNKNOW_EXCEPTION.getCode(), BizCodeEnume.UNKNOW_EXCEPTION.getMsg());
    }

}
