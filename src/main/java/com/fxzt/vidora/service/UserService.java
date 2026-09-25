package com.fxzt.vidora.service;

import com.fxzt.vidora.pojo.entity.User;
import com.fxzt.vidora.pojo.query.UserLoginDTo;
import com.fxzt.vidora.pojo.vo.UserLoginResult;

public interface UserService {

    /**
     * 注册用户
     */
    void register(User user);

    /**
     * 用户登录
     *
     */
    UserLoginResult login(UserLoginDTo userLoginDTo);
}
