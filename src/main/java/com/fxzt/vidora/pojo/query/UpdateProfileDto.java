package com.fxzt.vidora.pojo.query;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileDto {
    @Size(max = 20,message = "昵称不能超过 20 个字")
    private String nickname;            // 可选；为空串视为"不改"

    private String avatar;              // 可选:object key,为空串视为"不改"

    @AssertTrue(message = "昵称和头像至少要改一项")
    public boolean isAtLeastOnePresent() {
        return (nickname != null && !nickname.isBlank())
                || (avatar != null && !avatar.isBlank());
    }
}
