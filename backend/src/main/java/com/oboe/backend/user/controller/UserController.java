package com.oboe.backend.user.controller;

import com.oboe.backend.common.dto.ResponseDto;
import com.oboe.backend.user.dto.SignUpDto;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.service.UserService;
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
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User", description = "사용자 관련 API")
public class UserController {

  private final UserService userService;

  @Operation(summary = "회원가입", description = "SMS 인증 완료 후 새로운 사용자를 등록합니다.")
  @PostMapping(value = "/signup", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseDto<User>> signUp(@Valid @RequestBody SignUpDto dto) {
    log.info("회원가입 시작 - 이메일: {}, 닉네임: {}", dto.getEmail(), dto.getNickname());
    return ResponseEntity.ok(userService.signUp(dto));
  }

}
