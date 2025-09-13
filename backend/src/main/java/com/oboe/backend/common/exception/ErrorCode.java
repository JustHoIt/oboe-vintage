package com.oboe.backend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

  // 공통 오류
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
  INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),

  // 인증/인가 오류
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
  FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

  // 사용자 관련 오류
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
  USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 사용자입니다."),
  EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다."),
  USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 사용자명입니다."),
  NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 닉네임입니다."),
  PHONE_NUMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 전화번호입니다."),
  INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 올바르지 않습니다."),
  INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다."),
  SOCIAL_LOGIN_NOT_SUPPORTED(HttpStatus.FORBIDDEN, "소셜 로그인 사용자는 해당 기능을 이용할 수 없습니다."),
  USER_ALREADY_WITHDRAWN(HttpStatus.FORBIDDEN, "이미 탈퇴된 계정입니다."),
  USER_SUSPENDED(HttpStatus.FORBIDDEN, "정지된 계정입니다."),


  // 파일 관련 오류
  FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."),
  FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
  INVALID_FILE_FORMAT(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다."),

  // SMS 관련 오류
  SMS_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SMS 발송에 실패했습니다."),
  SMS_INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "올바르지 않은 휴대폰 번호입니다."),
  SMS_INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "인증번호를 입력해주세요."),
  SMS_VERIFICATION_EXPIRED(HttpStatus.BAD_REQUEST, "인증번호가 만료되었거나 존재하지 않습니다."),
  SMS_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "인증번호가 일치하지 않습니다."),
  SMS_VERIFICATION_REQUIRED(HttpStatus.BAD_REQUEST, "SMS 인증이 필요합니다."),
  SMS_QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "SMS 발송 한도를 초과했습니다."),
  SMS_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "SMS 서비스를 사용할 수 없습니다.");

  private final HttpStatus httpStatus;
  private final String message;
  
  ErrorCode(HttpStatus httpStatus, String message) {
    this.httpStatus = httpStatus;
    this.message = message;
  }
}
