package com.fxzt.vidora.pojo.query;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoCreateDTo {
    @NotBlank(message = "视频标题不能为空")
    private String title;               //视频标题
    private String introduction;        //视频简介
    @NotBlank
    private String coverKey;            //封面图片
    @NotBlank(message = "视频文件不能为空")
    private String videoKey;            //视频文件
    @NotBlank(message = "视频时长不能为空")
    private Integer duration;           //视频时长
}
