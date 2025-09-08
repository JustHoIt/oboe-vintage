package com.oboe.backend.message.controller;

import com.oboe.backend.message.dto.SmsAuthRequestDto;
import com.oboe.backend.message.service.MessageService;
import com.oboe.backend.common.dto.ResponseDto;
import com.oboe.backend.user.dto.FindPasswordDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/v1/message")
@RequiredArgsConstructor
@Tag(name = "SMS 전송", description = "SMS 발송 컨트롤러")
public class MessageController {

  private final MessageService messageService;

  // ✅회원가입 인증번호 발송
  @Operation(summary = "인증번호 발송", description = "입력받은 휴대폰 번호로 인증번호를 발송합니다.")
  @PostMapping(value = "/sendCertification", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseDto<String>> sendCertification(@RequestBody SmsAuthRequestDto dto) {
    log.info("=== SMS 인증번호 발송 요청 ===");
    log.info("휴대폰번호: {}", dto.getPhoneNumber());
    return ResponseEntity.ok(messageService.sendMessage(dto));
  }

  // ✅인증번호 확인
  @Operation(summary = "인증번호 검증", description = "입력받은 인증번호를 검증합니다.")
  @PostMapping(value = "/verifyCertification", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseDto<String>> verifyCertification(
      @RequestBody SmsAuthRequestDto dto) {
    log.info("=== SMS 인증번호 검증 요청 ===");
    log.info("휴대폰번호: {}, 인증번호: {}", dto.getPhoneNumber(), dto.getVerificationCode());
    return ResponseEntity.ok(messageService.verifyMessage(dto));
  }

  // ✅비밀번호 찾기 인증번호 발송
  @Operation(summary = "비밀번호 찾기 인증번호 발송", description = "이메일과 휴대폰번호로 계정을 검증한 후 인증번호를 발송합니다. OAuth2 계정은 제외됩니다.")
  @PostMapping(value = "/sendPasswordReset", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseDto<String>> sendPasswordResetSms(@Valid @RequestBody FindPasswordDto dto) {
    log.info("=== 비밀번호 찾기 SMS 인증번호 발송 요청 ===");
    log.info("이메일: {}, 휴대폰번호: {}", dto.getEmail(), dto.getPhoneNumber());
    return ResponseEntity.ok(messageService.sendPasswordResetMessage(dto));
  }

}
