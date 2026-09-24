package com.fxzt.vidora.service;

import com.fxzt.vidora.pojo.entity.User;
import com.fxzt.vidora.pojo.query.UserLoginDTo;
import jakarta.validation.Valid;

public interface UserService {

    /**
     * 注册用户
     */
    void register(User user);

    /**
     * 用户登录
     */
    void login(UserLoginDTo userLoginDTo);
}
