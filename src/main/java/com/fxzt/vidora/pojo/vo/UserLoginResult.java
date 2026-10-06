package com.fxzt.vidora.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class UserLoginResult {
    private String token;
    private String nickname;
    private Long id;
    private String avatarUrl;
}
