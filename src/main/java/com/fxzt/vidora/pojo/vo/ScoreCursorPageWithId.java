package com.fxzt.vidora.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScoreCursorPageWithId<T> {
    private List<T> items;
    private Boolean hasMore;
    private Long nextCursorScore;     // 分数型游标
    private Long nextCursorId;
}
