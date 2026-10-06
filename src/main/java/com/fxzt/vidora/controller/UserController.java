package com.fxzt.vidora.controller;

import com.fxzt.vidora.common.Result;
import com.fxzt.vidora.pojo.query.*;
import com.fxzt.vidora.pojo.vo.*;
import com.fxzt.vidora.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    public Result<Void> register(@Valid @RequestBody UserRegisterDto userRegisterDto){
        log.info("注册用户，用户信息{}", userRegisterDto.getUsername());
        userService.register(userRegisterDto);
        return Result.success();
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result<UserLoginResult> login(@Valid @RequestBody UserLoginDTo userLoginDTo){
        log.info("用户登录，用户信息{}", userLoginDTo.getUsername());
        UserLoginResult userLoginResult = userService.login(userLoginDTo);
        return Result.success(userLoginResult);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public Result<UserMeVo> me(){
        log.debug("获取当前用户信息");
        return Result.success(userService.getCurrentUser());
    }

    /**
     * 修改当前用户信息
     */
    @PutMapping("/profile")
    public Result<UserProfileVo> updateProfile(@Valid @RequestBody UpdateProfileDto dto){
        log.debug("修改当前用户信息{}", dto);
        return Result.success(userService.updateProfile(dto));
    }

    /**
     * 获取用户点赞视频列表
     */
    @GetMapping("/liked")
    public Result<CursorPage<VideoListItem>> liked(@Valid CursorPageDto cursorPageDto){
        log.debug("获取用户 liked 视频列表");
        CursorPage<VideoListItem> cursorPage = userService.liked(cursorPageDto);
        return Result.success(cursorPage);
    }

    /**
     * 获取用户收藏视频列表
     */
    @GetMapping("/favorites")
    public Result<CursorPage<VideoListItem>> favorite(@Valid CursorPageDto cursorPageDto){
        log.debug("获取用户 favorite 视频列表");
        CursorPage<VideoListItem> cursorPage = userService.favorite(cursorPageDto);
        return Result.success(cursorPage);
    }

    /**
     * 关注用户
     */
    @PostMapping("/{id}/follow")
    public Result<Void> follow(@PathVariable Long id){
        log.debug("关注用户，用户ID{}", id);
        userService.follow(id);
        return Result.success();
    }

    /**
     * 取消关注用户
     */
    @DeleteMapping("/{id}/follow")
    public Result<Void> unFollow(@PathVariable Long id){
        log.debug("取消关注用户，用户ID{}", id);
        userService.unFollow(id);
        return Result.success();
    }

    @GetMapping("/following")
    public Result<CursorPage<UserListItem>> following(@Valid CursorPageDto dto){
        log.debug("获取用户 following 列表");
        CursorPage<UserListItem> cursorPage = userService.following(dto);
        return Result.success(cursorPage);
    }

    @GetMapping("/history")
    public Result<ScoreCursorPage<HistoryItem>> history(@Valid HistoryPageDto dto){
        log.debug("获取用户 history 列表");
        ScoreCursorPage<HistoryItem> cursorPage = userService.history(dto);
        return Result.success(cursorPage);
    }
}
