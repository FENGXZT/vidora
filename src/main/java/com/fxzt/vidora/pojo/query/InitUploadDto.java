package com.fxzt.vidora.pojo.query;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InitUploadDto {
    @NotBlank(message = "视频标题不能为空")
    private String title;               //视频标题
    @NotBlank(message = "原始文件名不能为空")
    private String originalFilename;    //原始文件名
    @NotNull(message = "文件大小不能为空")
    private Long fileSize;              //文件大小
    @NotBlank(message = "内容类型不能为空")
    private String contentType;         //内容类型
    @NotNull(message = "视频时长不能为空")
    private Integer duration;            //视频时长
    @NotBlank(message = "视频封面不能为空")
    private String coverKey;                 //视频封面
    private String introduction;            //简介
    @NotBlank(message = "文件哈希值不能为空")
    private String fileHash;              //文件哈希值
}
