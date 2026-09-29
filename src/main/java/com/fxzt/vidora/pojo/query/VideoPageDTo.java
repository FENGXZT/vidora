package com.fxzt.vidora.pojo.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
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
    @Min(value = 1, message = "页码不能小于1")
    private Integer page=1;
    @Min(value = 1, message = "页大小不能小于1")
    @Max(value = 100, message = "页大小不能大于100")
    private Integer pageSize=10;

    @Size(max = 50, message = "关键字长度不能超过50")
    private String keyword;     // 按视频名称、描述模糊搜索

    //排序
    private String orderBy = "create_time";     // 按什么排序,默认按创建时间
    private String order = "desc";       // 升序还是降序，默认降序
}
