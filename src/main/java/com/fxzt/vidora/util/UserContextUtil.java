package com.fxzt.vidora.util;

import io.jsonwebtoken.Claims;

public class UserContextUtil {
    private static final ThreadLocal<Claims> USER_CONTEXT = new ThreadLocal<>();

    public static void set(Claims claims){
        USER_CONTEXT.set(claims);
    }

    public static Claims get(){
        return USER_CONTEXT.get();
    }

    public static void remove(){
        USER_CONTEXT.remove();
    }
}
