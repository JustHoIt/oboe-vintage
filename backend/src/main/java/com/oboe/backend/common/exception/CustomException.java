package com.oboe.backend.common.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

  private final ErrorCode errorCode;
  private final String detailMessage;

  public CustomException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
    this.detailMessage = errorCode.getMessage();
  }

  public CustomException(ErrorCode errorCode, String detailMessage) {
    super(detailMessage);
    this.errorCode = errorCode;
    this.detailMessage = detailMessage;
  }

  public CustomException(ErrorCode errorCode, Throwable cause) {
    super(errorCode.getMessage(), cause);
    this.errorCode = errorCode;
    this.detailMessage = errorCode.getMessage();
  }

  public CustomException(ErrorCode errorCode, String detailMessage, Throwable cause) {
    super(detailMessage, cause);
    this.errorCode = errorCode;
    this.detailMessage = detailMessage;
  }
}
