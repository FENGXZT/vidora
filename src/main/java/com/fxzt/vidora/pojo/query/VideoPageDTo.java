package com.fxzt.vidora.pojo.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 视频分页查询参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoPageDTo {
    private Integer page=1;
    private Integer pageSize=10;

    //TODO 添加查询参数，例如视频名称、视频描述等
    private Long userId;      // 按作者筛选
    private String title;     // 按标题模糊搜索
}
