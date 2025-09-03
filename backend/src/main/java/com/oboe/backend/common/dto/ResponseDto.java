package com.oboe.backend.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "API 응답 DTO")
public class ResponseDto<T> {

    @Schema(description = "응답 코드", example = "200")
    private int code;

    @Schema(description = "응답 메시지", example = "성공")
    private String message;

    @Schema(description = "응답 데이터")
    private T data;

    public static <T> ResponseDto<T> success(T data) {
        return ResponseDto.<T>builder()
                .code(200)
                .message("성공")
                .data(data)
                .build();
    }

    public static <T> ResponseDto<T> success(String message, T data) {
        return ResponseDto.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ResponseDto<T> error(int code, String message) {
        return ResponseDto.<T>builder()
                .code(code)
                .message(message)
                .build();
    }

    public boolean isSuccess() {
        return this.code == 200;
    }
}
