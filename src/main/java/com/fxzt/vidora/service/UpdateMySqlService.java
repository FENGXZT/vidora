package com.fxzt.vidora.service;

public interface UpdateMySqlService {

    /**
     * 更新视频播放量
     */
    void updateViewCount();

    /**
     * 更新视频点赞数
     */
    void updateLikeCount();

    /**
     * 全量更新视频点赞数
     */
    void updateLikeCountFullAmount();
}
