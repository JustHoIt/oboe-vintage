package com.oboe.backend.user.controller;

import com.oboe.backend.common.dto.ResponseDto;
import com.oboe.backend.user.dto.LoginDto;
import com.oboe.backend.user.dto.LoginResponseDto;
import com.oboe.backend.user.dto.SignUpDto;
import com.oboe.backend.user.dto.TokenRefreshDto;
import com.oboe.backend.user.dto.TokenResponseDto;
import com.oboe.backend.user.dto.UserProfileDto;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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

  @Operation(summary = "로그인", description = "이메일과 비밀번호로 사용자 로그인을 수행합니다.")
  @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseDto<LoginResponseDto>> login(@Valid @RequestBody LoginDto dto) {
    log.info("로그인 요청 - 이메일: {}", dto.getEmail());
    return ResponseEntity.ok(userService.login(dto));
  }

  @Operation(summary = "토큰 갱신", description = "Refresh Token을 사용하여 새로운 Access Token을 발급받습니다.")
  @PostMapping(value = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseDto<TokenResponseDto>> refreshToken(@Valid @RequestBody TokenRefreshDto dto) {
    log.info("토큰 갱신 요청");
    return ResponseEntity.ok(userService.refreshToken(dto));
  }

  @Operation(summary = "로그아웃", description = "사용자 로그아웃을 수행합니다.")
  @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseDto<String>> logout(@RequestHeader("Authorization") String authorization) {
    log.info("로그아웃 요청");
    return ResponseEntity.ok(userService.logout(authorization));
  }

  @Operation(summary = "현재 사용자 정보 조회", description = "JWT 토큰을 통해 현재 로그인한 사용자의 정보를 조회합니다.")
  @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseDto<UserProfileDto>> getCurrentUser(@RequestHeader("Authorization") String authorization) {
    log.info("현재 사용자 정보 조회 요청");
    return ResponseEntity.ok(userService.getCurrentUser(authorization));
  }

}
