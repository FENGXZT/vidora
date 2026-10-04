package com.fxzt.vidora.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Comment {
    private Long id;            // ID
    private Long userId;        // 用户ID
    private Long videoId;       // 视频ID
    private String content;     // 评论内容
    private Long createTime;    // 创建时间

    private String nickName;    // 用户昵称
    private String avatarUrl;   // 用户头像
}
