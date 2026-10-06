package com.fxzt.vidora.service;

import com.fxzt.vidora.pojo.query.*;
import com.fxzt.vidora.pojo.vo.*;
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

    /**
     * 获取用户点赞视频列表
     */
    CursorPage<VideoListItem> liked(CursorPageDto cursorPageDto);

    /**
     * 获取用户收藏视频列表
     */
    CursorPage<VideoListItem> favorite(CursorPageDto cursorPageDto);

    /**
     * 获取当前用户信息
     */
    UserMeVo getCurrentUser();

    /**
     * 修改当前用户信息
     */
    UserProfileVo updateProfile(UpdateProfileDto dto);

    /**
     * 关注用户
     */
    void follow(Long id);

    /**
     * 取消关注用户
     */
    void unFollow(Long id);

    /**
     * 获取用户关注列表
     */
    CursorPage<UserListItem> following(CursorPageDto dto);


    ScoreCursorPage<HistoryItem> history(@Valid HistoryPageDto dto);
}
