package com.oboe.backend.common.exception;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ErrorResponse {

  private final LocalDateTime timestamp;
  private final int status;
  private final String error;
  private final String message;
  private final String path;

  public static ErrorResponse of(ErrorCode errorCode, String detailMessage) {
    return ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(errorCode.getHttpStatus().value())
        .error(errorCode.getHttpStatus().getReasonPhrase())
        .message(detailMessage)
        .path("") // 실제 요청 경로는 GlobalExceptionHandler에서 설정
        .build();
  }

  public static ErrorResponse of(ErrorCode errorCode) {
    return ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(errorCode.getHttpStatus().value())
        .error(errorCode.getHttpStatus().getReasonPhrase())
        .message(errorCode.getMessage())
        .path("")
        .build();
  }
}
