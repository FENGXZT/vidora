package com.fxzt.vidora.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class HistoryItem {
    private Long id;
    private String title;
    private String coverUrl;
    private LocalDateTime viewTime;
}
