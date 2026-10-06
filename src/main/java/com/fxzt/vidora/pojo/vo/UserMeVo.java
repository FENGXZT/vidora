package com.fxzt.vidora.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserMeVo {
    private String nickname;
    private String avatarUrl;
    private Long id;
    private String username;
}
