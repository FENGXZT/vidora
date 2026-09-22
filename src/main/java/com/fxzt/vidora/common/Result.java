package com.fxzt.vidora.common;

import lombok.Data;

/*
*统一封装返回结果
*/
@Data
public class Result<T> {
    private T data;
    private int code;
    private String message;

    public static <E> Result<E> success(E data) {
        Result<E> result = new Result<>();
        result.setData(data);
        result.setCode(1);
        result.setMessage("success");
        return result;
    }

    public static Result<Void> success() {
        return success(null);
    }

    public static <E> Result<E> error(String msg){

        Result<E> result = new Result<>();
        result.setCode(0);
        result.setMessage(msg);
        return result;
    }
}
