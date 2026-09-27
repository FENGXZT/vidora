package com.fxzt.vidora.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InitUploadVo {
    private Long id;               // 视频ID
    private String videoKey;           // 对象键
    private String uploadId;            // 上传ID
    private Integer partSize;          //分片大小
    private List<Integer> uploadedParts;   //已上传的分片
}
