package com.fxzt.vidora.exception;

import com.fxzt.vidora.common.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    //    细分异常处理，接收指定异常
    /**
     * 处理参数校验异常（@Validated + @RequestParam）
     * @param e 方法参数校验异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<String>> handlerConstraintViolation(ConstraintViolationException e){
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}" , msg);
        return ResponseEntity.status(400)
                .body(Result.error(msg));
    }

    /**
     * 处理参数校验异常（@Valid @RequestBody）
     * @param e 请求体参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<String>> handlerMethodArgumentNotValid(MethodArgumentNotValidException e){
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(e1 -> e1.getField() + " " + e1.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}" , msg);
        return ResponseEntity.status(400)
                .body(Result.error(msg));
    }

    /** 参数类型不匹配，如 /video/abc 里 abc 转 Long 失败 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String msg = "参数 " + e.getName() + " 类型不正确";
        log.warn(msg);
        return ResponseEntity.status(400)
                .body(Result.error(msg));
    }

    /** 请求体 JSON 解析失败 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return ResponseEntity.status(400)
                .body(Result.error("请求体格式错误"));
    }

        /**
         * 处理权限异常
         * @param e 权限异常
         */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Result<String>> handlerUnauthorized(UnauthorizedException e){
        log.warn("出现权限异常{}" , e.getMessage());
        return ResponseEntity.status(401)
                .body(Result.error("权限异常： " + e.getMessage()));
    }

    /**
     * 处理请求方式不支持异常
     * @param e 请求方式不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<String>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方式错误{}" , e.getMethod());
        return ResponseEntity.status(405)
                .body(Result.error("请求方式不支持: " + e.getMethod()));
    }

    /**
     * 处理文件上传异常
     * @param e 异常
     */
    @ExceptionHandler(UploadException.class)
    public ResponseEntity<Result<String>> handleUploadException(UploadException e) {
        log.warn("文件上传异常{}" , e.getMessage());
        return ResponseEntity.status(e.getCode())
                .body(Result.error("上传失败： " + e.getMessage()));
    }

    /** 资源不存在，如 /nonexistent */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("资源不存在: {}", e.getResourcePath());
        return ResponseEntity.status(404)
                .body(Result.error("资源不存在"));
    }

    /**
     * 处理业务异常
     * @param e 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<String> handlerBusinessException(BusinessException e){
        log.warn("出现异常 {}" , e.getMessage());
        return Result.error("业务异常： " + e.getMessage());
    }

    // ========== 5xx：服务端错误 ==========

    /*    兜底异常处理，接收所有未处理异常*/
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<String>> exceptionHandler(Exception e){
        log.error("出现异常" , e);
        return ResponseEntity.status(500)
                .body(Result.error("系统繁忙，请稍后重试" ));
    }
}
