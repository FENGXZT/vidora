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

    /**
     * 更新视频收藏数
     */
    void updateFavoriteCount();

    /**
     * 全量更新视频收藏数
     */
    void updateFavoriteCountFullAmount();

    /**
     * 重新计算热门视频分数
     */
    void recomputeHotScore();
}
