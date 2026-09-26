package com.fxzt.vidora.pojo.query;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRegisterDto {
    @NotBlank(message = "登录账号不能为空")
    private String username;    //登录账号
    @NotBlank(message = "密码不能为空")
    private String password;    //密码
    @NotBlank(message = "昵称不能为空")
    private String nickname;    //昵称
    private String avatar;      //头像
}
