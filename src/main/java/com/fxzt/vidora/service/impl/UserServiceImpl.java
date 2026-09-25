package com.fxzt.vidora.service.impl;

import com.fxzt.vidora.exception.BusinessException;
import com.fxzt.vidora.mapper.UserMapper;
import com.fxzt.vidora.pojo.entity.User;
import com.fxzt.vidora.pojo.query.UserLoginDTo;
import com.fxzt.vidora.pojo.vo.UserLoginResult;
import com.fxzt.vidora.service.UserService;
import com.fxzt.vidora.util.JwtUtil;
import com.fxzt.vidora.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public void register(User user) {
        //判断用户名是否已存在
        if (userMapper.findByUsername(user.getUsername()) !=null ) throw new BusinessException("该用户名已存在");

        //密码加密
        user.setPassword(PasswordUtil.encrypt(user.getPassword()));

        //保存用户信息
        userMapper.insert(user);

    }

    @Override
    public UserLoginResult login(UserLoginDTo userLoginDTo) {
        //判断用户名是否存在
        User user = userMapper.findByUsername(userLoginDTo.getUsername());
        if (user == null) throw new BusinessException("用户名或密码错误");
        //判断密码是否正确
        Boolean isPasswordCorrect = PasswordUtil.match(userLoginDTo.getPassword(), user.getPassword());
        if (!isPasswordCorrect) throw new BusinessException("用户名或密码错误");
        //生成token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        //封装用户信息
        UserLoginResult userLoginResult = new UserLoginResult(token, user.getNickname(), user.getId(), user.getAvatar());
        return userLoginResult;
    }
}
