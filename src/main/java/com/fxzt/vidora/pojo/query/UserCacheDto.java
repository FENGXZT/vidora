package com.fxzt.vidora.pojo.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserCacheDto {
    private Long id;
    private String nickname;
    private String avatar;

    private LocalDateTime cursorTime;   // 游标时间
    private Long cursorId;      // 游标ID
}
