package com.fxzt.vidora.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserLoginResult {
    private String token;
    private String nickname;
    private Long id;
    private String avatarUrl;
}
