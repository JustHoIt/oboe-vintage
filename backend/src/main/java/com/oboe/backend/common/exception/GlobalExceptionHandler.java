package com.oboe.backend.common.exception;

import com.oboe.backend.common.dto.ResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * 커스텀 예외 처리
   */
  @ExceptionHandler(CustomException.class)
  public ResponseEntity<ResponseDto<Void>> handleCustomException(CustomException e) {
    log.warn("CustomException: {}", e.getMessage());
    ResponseDto<Void> response = ResponseDto.error(e.getErrorCode().getHttpStatus().value(),
        e.getDetailMessage());
    return ResponseEntity.status(e.getErrorCode().getHttpStatus()).body(response);
  }

  /**
   * @Valid 검증 실패 예외 처리
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ResponseDto<Void>> handleValidationException(
      MethodArgumentNotValidException e) {
    log.warn("ValidationException: {}", e.getMessage());

    Map<String, String> errors = new HashMap<>();
    e.getBindingResult().getAllErrors().forEach((error) -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      errors.put(fieldName, errorMessage);
    });

    ResponseDto<Void> response = ResponseDto.error(
        ErrorCode.INVALID_INPUT_VALUE.getHttpStatus().value(), errors.toString());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  /**
   * @ModelAttribute 검증 실패 예외 처리
   */
  @ExceptionHandler(BindException.class)
  public ResponseEntity<ResponseDto<Void>> handleBindException(BindException e) {
    log.warn("BindException: {}", e.getMessage());

    Map<String, String> errors = new HashMap<>();
    e.getBindingResult().getAllErrors().forEach((error) -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      errors.put(fieldName, errorMessage);
    });

    ResponseDto<Void> response = ResponseDto.error(
        ErrorCode.INVALID_INPUT_VALUE.getHttpStatus().value(), errors.toString());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  /**
   * 타입 변환 실패 예외 처리
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ResponseDto<Void>> handleTypeMismatchException(
      MethodArgumentTypeMismatchException e) {
    log.warn("TypeMismatchException: {}", e.getMessage());
    ResponseDto<Void> response = ResponseDto.error(
        ErrorCode.INVALID_INPUT_VALUE.getHttpStatus().value(),
        "잘못된 타입의 파라미터입니다: " + e.getName());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  /**
   * IllegalArgumentException 처리
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ResponseDto<Void>> handleIllegalArgumentException(
      IllegalArgumentException e) {
    log.warn("IllegalArgumentException: {}", e.getMessage());
    ResponseDto<Void> response = ResponseDto.error(
        ErrorCode.INVALID_INPUT_VALUE.getHttpStatus().value(), e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  /**
   * 모든 예외 처리 (최종 fallback)
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ResponseDto<Void>> handleException(Exception e) {
    log.error("UnexpectedException: ", e);
    ResponseDto<Void> response = ResponseDto.error(
        ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus().value(),
        "예상치 못한 오류가 발생했습니다.");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }
}
