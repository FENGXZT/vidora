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
    private String keyword;     // 按视频名称、描述模糊搜索

    //排序
    private String orderBy = "create_time";     // 按什么排序,默认按创建时间
    private String order = "desc";       // 升序还是降序，默认降序
}
