package com.fxzt.vidora.service.impl;

import com.fxzt.vidora.exception.BusinessException;
import com.fxzt.vidora.mapper.UserMapper;
import com.fxzt.vidora.pojo.entity.User;
import com.fxzt.vidora.pojo.query.UserLoginDTo;
import com.fxzt.vidora.pojo.query.UserRegisterDto;
import com.fxzt.vidora.pojo.vo.UserLoginResult;
import com.fxzt.vidora.service.FileService;
import com.fxzt.vidora.service.UserService;
import com.fxzt.vidora.util.JwtUtil;
import com.fxzt.vidora.util.PasswordUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private FileService fileService;

    @Value("${minio.ImageBucketName}")
    private String imageBucket;

    @Override
    public void register(@Valid UserRegisterDto userRegisterDto) {
        //判断用户名是否已存在
        if (userMapper.findByUsername(userRegisterDto.getUsername()) !=null ) throw new BusinessException("该用户名已存在");

        //密码加密
        userRegisterDto.setPassword(PasswordUtil.encrypt(userRegisterDto.getPassword()));

        //保存用户信息
        userMapper.insert(userRegisterDto);

    }

    @Override
    public UserLoginResult login(UserLoginDTo userLoginDTo) {
        //判断用户名是否存在
        User user = userMapper.findByUsername(userLoginDTo.getUsername());
        if (user == null) throw new BusinessException("用户名或密码错误");
        //判断密码是否正确
        if (!PasswordUtil.match(userLoginDTo.getPassword(), user.getPassword())) throw new BusinessException("用户名或密码错误");
        //生成token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        //封装用户信息
        String avatarUrl = user.getAvatar() != null ? fileService.presign(imageBucket,user.getAvatar(),10): null;
        return new UserLoginResult(token, user.getNickname(), user.getId(), avatarUrl);
    }
}
