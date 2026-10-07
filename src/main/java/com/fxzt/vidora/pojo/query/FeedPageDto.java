package com.fxzt.vidora.pojo.query;

import com.fxzt.vidora.exception.BusinessException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FeedPageDto {
    private String sort="latest";       //排序方式
    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 50, message = "每页条数不能大于50")
    private Integer size=20;           //每页条数
    /** 仅 sort=latest 用：上一页最后一条的 createTime */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime cursorTime;  //时间戳
    /** 仅 sort=hot 用, 上一页最后一条的 score */
    private Long cursorScore;           //分数
    private Long cursorId;             //ID

    private static final Set<String> ALLOWED_SORT = Set.of("latest", "hot");

    public void normalize() {
        if (sort == null || sort.isBlank()) { sort = "latest"; }
        else if (!ALLOWED_SORT.contains(sort)) { throw new BusinessException("sort 参数非法：" + sort); }
        size = (size == null) ? 20 : Math.clamp(size, 1, 50);
    }

    public void verifyCursor(){
        if ("latest".equals(sort)){
            boolean hasTime =cursorTime != null;
            boolean hasId   = cursorId != null;
            if (hasTime != hasId)
                throw new BusinessException("游标参数不完整：cursorTime 与 cursorId 必须同时提供");
        }else {
            boolean hasScore = cursorScore != null;
            boolean hasId = cursorId != null;
            if (hasScore != hasId)
                throw new BusinessException("游标参数不完整：cursorScore 与 cursorId 必须同时提供");
        }
    }
}
