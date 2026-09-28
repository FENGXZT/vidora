package com.fxzt.vidora.pojo.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InitUploadDto {
    private String title;               //视频标题
    private String originalFilename;    //原始文件名
    private Long fileSize;              //文件大小
    private String contentType;         //内容类型
    private Integer duration;            //视频时长
    private String coverKey;                 //视频封面
    private String introduction;            //简介
    private String fileHash;              //文件哈希值
}
