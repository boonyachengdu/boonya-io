package com.boonya.lab.io.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页响应结果")
public class PageResult<T> {

    @Schema(description = "当前页码", example = "1")
    private Long current;

    @Schema(description = "每页数量", example = "10")
    private Long size;

    @Schema(description = "总记录数", example = "100")
    private Long total;

    @Schema(description = "总页数", example = "10")
    private Long pages;

    @Schema(description = "数据列表")
    private java.util.List<T> records;

    public static <T> PageResult<T> of(long current, long size, long total, java.util.List<T> records) {
        long pages = (total + size - 1) / size;
        return PageResult.<T>builder()
                .current(current)
                .size(size)
                .total(total)
                .pages(pages)
                .records(records)
                .build();
    }
}
