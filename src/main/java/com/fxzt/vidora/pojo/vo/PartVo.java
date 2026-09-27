package com.fxzt.vidora.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PartVo {
    private Integer partNumber;     // 分片编号
    private String etag;           // 分片etag
}
