package com.fxzt.vidora.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ScoreCursorPage<T> {
    private List<T> items;
    private Boolean hasMore;
    private Long nextCursorScore;
}
