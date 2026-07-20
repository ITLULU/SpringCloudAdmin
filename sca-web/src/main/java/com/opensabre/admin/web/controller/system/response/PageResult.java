package com.opensabre.admin.web.controller.system.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页结果响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页结果响应")
public class PageResult<T> {

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "数据列表")
    private List<T> rows;
}
