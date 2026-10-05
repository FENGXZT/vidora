package com.fxzt.vidora.pojo.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UploadContext {
    private Long userId;              // 用户ID
    private String uploadId;            // 上传ID
    private String videoKey;           // 对象键
    private Long totalSize;            // 文件总大小
}
