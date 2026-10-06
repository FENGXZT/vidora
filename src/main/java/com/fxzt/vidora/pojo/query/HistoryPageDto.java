package com.fxzt.vidora.pojo.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HistoryPageDto {
    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 50, message = "每页条数不能大于50")
    private Integer size = 20;

    private Long cursorScore;

    public void normalize() {
        size = (size == null) ? 20 : Math.clamp(size, 1, 50);
    }
}
