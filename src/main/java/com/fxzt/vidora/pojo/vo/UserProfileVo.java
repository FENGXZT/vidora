package com.fxzt.vidora.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileVo {
    private Long id;
    private String avatarUrl;
    private String nickname;
}
