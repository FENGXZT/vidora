package com.fxzt.vidora.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
/**
 * 数值型游标不在这里
 * 本类被 feed 与旧缓存 JSON 复用
 */
public class CursorPage<T> {
    private List<T> items;
    private Boolean hasMore;
    private LocalDateTime nextCursor;     // 时间型游标
    private Long nextCursorId;
}
