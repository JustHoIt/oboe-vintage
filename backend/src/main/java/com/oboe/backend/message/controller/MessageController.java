package com.oboe.backend.message.controller;

import com.oboe.backend.common.dto.ResponseDto;
import com.oboe.backend.message.dto.SmsAuthRequestDto;
import com.oboe.backend.message.service.MessageService;
import com.oboe.backend.user.dto.FindPasswordDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
  @Operation(
      summary = "인증번호 발송",
      description = "입력받은 휴대폰 번호로 6자리 인증번호를 발송합니다. 3분간 유효하며, 회원가입 시 사용됩니다.",
      responses = {
          @ApiResponse(responseCode = "200", description = "인증번호 발송 성공"),
          @ApiResponse(responseCode = "400", description = "잘못된 휴대폰 번호 형식"),
          @ApiResponse(responseCode = "500", description = "SMS 발송 실패 또는 서버 오류")
      }
  )
  @PostMapping(value = "/sendCertification", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseDto<String>> sendCertification(@RequestBody SmsAuthRequestDto dto) {
    return ResponseEntity.ok(messageService.sendMessage(dto));
  }

  // ✅인증번호 확인
  @Operation(
      summary = "인증번호 검증",
      description = "입력받은 인증번호를 검증합니다. 발송된 인증번호와 일치하고 3분 이내여야 성공합니다.",
      responses = {
          @ApiResponse(responseCode = "200", description = "인증번호 검증 성공"),
          @ApiResponse(responseCode = "400", description = "잘못된 입력값 또는 만료된 인증번호"),
          @ApiResponse(responseCode = "401", description = "인증번호 불일치"),
          @ApiResponse(responseCode = "404", description = "발송되지 않은 휴대폰 번호")
      }
  )
  @PostMapping(value = "/verifyCertification", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseDto<String>> verifyCertification(
      @RequestBody SmsAuthRequestDto dto) {
    return ResponseEntity.ok(messageService.verifyMessage(dto));
  }

  // ✅비밀번호 찾기 인증번호 발송
  @Operation(
      summary = "비밀번호 찾기 인증번호 발송",
      description = "이메일과 휴대폰번호로 계정을 검증한 후 인증번호를 발송합니다. OAuth2 계정은 제외됩니다. 3분간 유효합니다.",
      responses = {
          @ApiResponse(responseCode = "200", description = "인증번호 발송 성공"),
          @ApiResponse(responseCode = "400", description = "잘못된 입력값"),
          @ApiResponse(responseCode = "404", description = "해당 정보로 등록된 사용자가 없음"),
          @ApiResponse(responseCode = "403", description = "비활성화된 계정 또는 OAuth2 계정"),
          @ApiResponse(responseCode = "500", description = "SMS 발송 실패 또는 서버 오류")
      }
  )
  @PostMapping(value = "/sendPasswordReset", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseDto<String>> sendPasswordResetSms(
      @Valid @RequestBody FindPasswordDto dto) {
    return ResponseEntity.ok(messageService.sendPasswordResetMessage(dto));
  }

}
