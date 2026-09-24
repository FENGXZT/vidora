package com.fxzt.vidora.controller;

import com.fxzt.vidora.common.Result;
import com.fxzt.vidora.pojo.entity.User;
import com.fxzt.vidora.pojo.query.UserLoginDTo;
import com.fxzt.vidora.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 注册
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody User user){
        log.info("注册用户，用户信息{}", user);
        userService.register(user);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<Void> login(@Valid UserLoginDTo userLoginDTo){
        log.info("用户登录，用户信息{}", userLoginDTo);
        userService.login(userLoginDTo);
        return Result.success();
    }
}
