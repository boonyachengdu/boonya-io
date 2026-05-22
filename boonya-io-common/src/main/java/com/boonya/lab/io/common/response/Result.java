package com.boonya.lab.io.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统一响应结果")
public class Result<T> {

    @Schema(description = "响应码", example = "200")
    private Integer code;

    @Schema(description = "响应消息", example = "success")
    private String message;

    @Schema(description = "响应数据")
    private T data;

    @Schema(description = "时间戳", example = "1234567890")
    private Long timestamp;

    public static <T> Result<T> success() {
        return Result.<T>builder()
                .code(HttpStatus.OK.value())
                .message("success")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(HttpStatus.OK.value())
                .message("success")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> Result<T> success(String message, T data) {
        return Result.<T>builder()
                .code(HttpStatus.OK.value())
                .message(message)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> Result<T> error(String message) {
        return Result.<T>builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> Result<T> error(Integer code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> Result<T> error(HttpStatus status, String message) {
        return Result.<T>builder()
                .code(status.value())
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public boolean isSuccess() {
        return this.code != null && this.code == HttpStatus.OK.value();
    }
}
