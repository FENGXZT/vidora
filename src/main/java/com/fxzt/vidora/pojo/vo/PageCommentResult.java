package com.fxzt.vidora.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageCommentResult<T> {
    private List<T> items;
    private Boolean hasMore;
    private LocalDateTime nextCursor;
    private Long nextCursorId;
}
