package com.fxzt.vidora.pojo.query;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class VideoMyPageDto extends BasePageDto {
    @Pattern(
            regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9 _\\-.]*$",
            message = "关键字只能包含中文、字母、数字、空格、下划线、横线和点"
    )
    @Size(max = 50, message = "关键字长度不能超过50")
    private String keyword;     // 按视频名称、描述模糊搜索

    private Integer status;     // 视频状态
}
