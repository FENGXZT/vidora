package com.fxzt.vidora.pojo.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentCacheDto {
    private Long id;            // ID
    private String content;     // 评论内容
    private LocalDateTime createTime;    // 创建时间

    private String nickName;    // 用户昵称
    private String avatarKey;   // 用户头像
}
