package com.fxzt.vidora.pojo.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BasePageDto {
    @Min(value = 1, message = "页码不能小于1")
    private Integer page = 1;                       // 页码
    @Min(value = 1, message = "页大小不能小于1")
    @Max(value = 100, message = "页大小不能大于100")
    private Integer pageSize = 10;                  // 页大小
    private String orderBy = "create_time";        // 排序字段
    private String order = "desc";                 // 排序方式

    private static final Set<String> ALLOWED_ORDER_BY =
            Set.of("create_time", "like_count", "view_count", "favorite_count");
    private static final Set<String> ALLOWED_ORDER = Set.of("asc", "desc");

    /** 参数安全校验 */
    public void normalize() {
        page = (page == null) ? 1 : page;
        pageSize = (pageSize == null) ? 10 : Math.min(pageSize, 100);
        if (!ALLOWED_ORDER_BY.contains(orderBy)) orderBy = "create_time";
        if (!ALLOWED_ORDER.contains(order)) order = "desc";
    }
}