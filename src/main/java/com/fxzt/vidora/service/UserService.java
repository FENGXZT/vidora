package com.fxzt.vidora.service;

import com.fxzt.vidora.pojo.query.UserLoginDTo;
import com.fxzt.vidora.pojo.query.UserRegisterDto;
import com.fxzt.vidora.pojo.vo.UserLoginResult;
import jakarta.validation.Valid;

public interface UserService {

    /**
     * 注册用户
     */
    void register(@Valid UserRegisterDto userRegisterDto);

    /**
     * 用户登录
     *
     */
    UserLoginResult login(UserLoginDTo userLoginDTo);
}
