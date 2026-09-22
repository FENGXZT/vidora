package com.fxzt.vidora.exception;

import com.fxzt.vidora.common.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

//    兜底异常处理，接收所有异常
    @ExceptionHandler(Exception.class)
    public Result<String> exceptionHandler(Exception e){
        log.error("出现异常" , e);
        return Result.error("出现异常： " + e.getMessage());
    }

    //    分细异常处理，接收指定异常
    /**
     * 处理参数校验异常
     * @param e 方法参数校验异常
     * @return
     */

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<String> exceptionHandler(ConstraintViolationException e){
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.error("出现异常 {}" , msg);
        return Result.error("出现异常： " + msg);
    }

    /**
     * 处理参数校验异常
     * @param e 请求体参数校验异常
     * @return
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> exceptionHandler(MethodArgumentNotValidException e){
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(e1 -> e1.getField() + " " + e1.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.error("出现异常 {}" , msg);
        return Result.error("出现异常： " + msg);
    }

    @ExceptionHandler(BusinessException.class)
    public Result<String> exceptionHandler(BusinessException e){
        log.error("出现异常 " , e);
        return Result.error("业务异常： " + e.getMessage());
    }
}
