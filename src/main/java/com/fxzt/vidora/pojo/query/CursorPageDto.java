package com.fxzt.vidora.pojo.query;

import com.fxzt.vidora.exception.BusinessException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CursorPageDto {
    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 50, message = "每页条数不能大于50")
    private Integer size=20;           //每页条数
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime cursorTime;  //时间戳
    private Long cursorId;             //ID

    public void verifyCursor(){
        boolean hasTime =cursorTime != null;
        boolean hasId   = cursorId != null;
        if (hasTime != hasId) throw new BusinessException("游标参数不完整：cursorTime 与 cursorId 必须同时提供");
    }

    public void normalize() {
        size = (size == null) ? 20 : Math.clamp(size, 1, 50);
    }
}
