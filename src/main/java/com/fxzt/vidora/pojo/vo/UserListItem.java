package com.fxzt.vidora.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserListItem {
    private Long id;            // 用户ID
    private String nickname;    // 用户昵称
    private String avatarUrl;   // 用户头像URL

    private LocalDateTime followTime; // 关注时间
}
