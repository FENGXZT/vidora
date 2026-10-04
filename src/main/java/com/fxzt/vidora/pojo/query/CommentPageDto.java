package com.fxzt.vidora.pojo.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentPageDto {
    private LocalDateTime cursorTime;   // 游标：上一页最后一条的时间
    private Long cursorId;              // 见下方"复合游标"
    @Max(value = 100)
    @Min(value = 1, message = "分页大小不能小于1")
    private Integer size = 20;
}