package com.oboe.backend.user.controller;

import com.oboe.backend.common.dto.ResponseDto;
import com.oboe.backend.user.dto.FindIdDto;
import com.oboe.backend.user.dto.FindIdResponseDto;
import com.oboe.backend.user.dto.LoginDto;
import com.oboe.backend.user.dto.LoginResponseDto;
import com.oboe.backend.user.dto.PasswordChangeDto;
import com.oboe.backend.user.dto.ResetPasswordDto;
import com.oboe.backend.user.dto.SignUpDto;
import com.oboe.backend.user.dto.TokenRefreshDto;
import com.oboe.backend.user.dto.TokenResponseDto;
import com.oboe.backend.user.dto.UserProfileDto;
import com.oboe.backend.user.dto.UserUpdateDto;
import com.oboe.backend.user.dto.WithdrawDto;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User", description = "사용자 관련 API")
public class UserController {

  private final UserService userService;

  @Operation(
      summary = "회원가입", 
      description = "SMS 인증 완료 후 새로운 사용자를 등록합니다. 이메일, 닉네임, 휴대폰번호 중복 검사가 수행됩니다.",
      responses = {
          @ApiResponse(responseCode = "200", description = "회원가입 성공"),
          @ApiResponse(responseCode = "400", description = "입력값 오류 또는 SMS 인증 미완료"),
          @ApiResponse(responseCode = "409", description = "이미 존재하는 이메일, 닉네임 또는 휴대폰번호")
      }
  )
  @PostMapping(value = "/signup", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseDto<User>> signUp(@Valid @RequestBody SignUpDto dto) {
    return ResponseEntity.ok(userService.signUp(dto));
  }

  @Operation(
      summary = "로그인", 
      description = "이메일과 비밀번호로 사용자 로그인을 수행합니다. 성공 시 JWT 액세스 토큰과 리프레시 토큰을 반환합니다.",
      responses = {
          @ApiResponse(responseCode = "200", description = "로그인 성공"),
          @ApiResponse(responseCode = "400", description = "입력값 오류"),
          @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치"),
          @ApiResponse(responseCode = "403", description = "비활성화된 계정")
      }
  )
  @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseDto<LoginResponseDto>> login(@Valid @RequestBody LoginDto dto) {
    return ResponseEntity.ok(userService.login(dto));
  }

  @Operation(
      summary = "토큰 갱신", 
      description = "Refresh Token을 사용하여 새로운 Access Token을 발급받습니다. Refresh Token이 유효해야 합니다.",
      responses = {
          @ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
          @ApiResponse(responseCode = "400", description = "입력값 오류"),
          @ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 Refresh Token"),
          @ApiResponse(responseCode = "403", description = "비활성화된 계정"),
          @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
      }
  )
  @PostMapping(value = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseDto<TokenResponseDto>> refreshToken(
      @Valid @RequestBody TokenRefreshDto dto) {
    return ResponseEntity.ok(userService.refreshToken(dto));
  }

  @Operation(
      summary = "로그아웃", 
      description = "사용자 로그아웃을 수행합니다. JWT 토큰이 필요합니다.",
      responses = {
          @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
          @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰")
      }
  )
  @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseDto<String>> logout(
      @RequestHeader("Authorization") String authorization) {
    return ResponseEntity.ok(userService.logout(authorization));
  }

  @Operation(
      summary = "현재 사용자 정보 조회", 
      description = "JWT 토큰을 통해 현재 로그인한 사용자의 상세 정보를 조회합니다.",
      responses = {
          @ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공"),
          @ApiResponse(responseCode = "401", description = "인증 실패 또는 만료된 토큰"),
          @ApiResponse(responseCode = "403", description = "비활성화된 계정"),
          @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
      }
  )
  @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseDto<UserProfileDto>> getCurrentUser(
      @RequestHeader("Authorization") String authorization) {
    return ResponseEntity.ok(userService.getCurrentUser(authorization));
  }

  @Operation(
      summary = "사용자 정보 수정", 
      description = "현재 로그인한 사용자의 정보를 수정합니다. (닉네임, 주소, 프로필 이미지) - 이메일과 비밀번호는 별도 API 사용",
      responses = {
          @ApiResponse(responseCode = "200", description = "사용자 정보 수정 성공"),
          @ApiResponse(responseCode = "400", description = "입력값 오류"),
          @ApiResponse(responseCode = "401", description = "인증 실패 또는 만료된 토큰"),
          @ApiResponse(responseCode = "403", description = "비활성화된 계정"),
          @ApiResponse(responseCode = "409", description = "닉네임 중복")
      }
  )

  @PutMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseDto<UserProfileDto>> updateUser(
      @RequestHeader("Authorization") String authorization,
      @Valid @RequestBody UserUpdateDto dto) {
    return ResponseEntity.ok(userService.updateUser(authorization, dto));
  }

  @Operation(
      summary = "비밀번호 변경", 
      description = "현재 로그인한 사용자의 비밀번호를 변경합니다. 현재 비밀번호 확인이 필요합니다.",
      responses = {
          @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
          @ApiResponse(responseCode = "400", description = "입력값 오류"),
          @ApiResponse(responseCode = "401", description = "인증 실패 또는 잘못된 현재 비밀번호"),
          @ApiResponse(responseCode = "403", description = "비활성화된 계정")
      }
  )
  @PutMapping(value = "/me/password", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseDto<String>> changePassword(
      @RequestHeader("Authorization") String authorization,
      @Valid @RequestBody PasswordChangeDto dto) {
    return ResponseEntity.ok(userService.changePassword(authorization, dto));
  }

  @Operation(
      summary = "프로필 이미지 업로드", 
      description = "현재 로그인한 사용자의 프로필 이미지를 업로드합니다. 파일 경로를 전달받아 저장합니다.",
      responses = {
          @ApiResponse(responseCode = "200", description = "프로필 이미지 업로드 성공"),
          @ApiResponse(responseCode = "400", description = "잘못된 파일 경로"),
          @ApiResponse(responseCode = "401", description = "인증 실패 또는 만료된 토큰"),
          @ApiResponse(responseCode = "403", description = "비활성화된 계정")
      }
  )
  @PostMapping(value = "/me/profile-image", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseDto<String>> uploadProfileImage(
      @RequestHeader("Authorization") String authorization,
      @RequestParam("filePath") String filePath) {
    return ResponseEntity.ok(userService.uploadProfileImage(authorization, filePath));
  }

  @Operation(
      summary = "아이디 찾기", 
      description = "이름과 휴대폰번호로 아이디(이메일)를 찾습니다. OAuth2 계정은 제외됩니다.",
      responses = {
          @ApiResponse(responseCode = "200", description = "아이디 찾기 성공"),
          @ApiResponse(responseCode = "400", description = "입력값 오류"),
          @ApiResponse(responseCode = "404", description = "해당 정보로 등록된 사용자가 없음"),
          @ApiResponse(responseCode = "403", description = "비활성화된 계정")
      }
  )
  @PostMapping(value = "/find-id", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseDto<FindIdResponseDto>> findId(@Valid @RequestBody FindIdDto dto) {
    log.info("아이디 찾기 요청 - 이름: {}", dto.getName());
    return ResponseEntity.ok(userService.findId(dto));
  }

  @Operation(
      summary = "비밀번호 재설정", 
      description = "SMS 인증 완료 후 새로운 비밀번호로 재설정합니다. OAuth2 계정은 제외됩니다.",
      responses = {
          @ApiResponse(responseCode = "200", description = "비밀번호 재설정 성공"),
          @ApiResponse(responseCode = "400", description = "입력값 오류"),
          @ApiResponse(responseCode = "401", description = "SMS 인증 미완료"),
          @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
          @ApiResponse(responseCode = "403", description = "비활성화된 계정")
      }
  )
  @PostMapping(value = "/reset-password", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseDto<String>> resetPassword(
      @Valid @RequestBody ResetPasswordDto dto) {
    log.info("비밀번호 재설정 요청 - 이메일: {}", dto.getEmail());
    return ResponseEntity.ok(userService.resetPassword(dto.getEmail(), dto.getNewPassword()));
  }

  @Operation(
      summary = "회원탈퇴", 
      description = "현재 로그인한 사용자의 계정을 탈퇴 처리합니다. 30일 후 개인정보가 완전히 삭제됩니다.",
      responses = {
          @ApiResponse(responseCode = "200", description = "회원탈퇴 성공"),
          @ApiResponse(responseCode = "400", description = "입력값 오류"),
          @ApiResponse(responseCode = "401", description = "인증 실패 또는 잘못된 비밀번호"),
          @ApiResponse(responseCode = "403", description = "비활성화된 계정")
      }
  )
  @DeleteMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseDto<String>> withdrawUser(
      @RequestHeader("Authorization") String authorization,
      @Valid @RequestBody WithdrawDto dto) {
    return ResponseEntity.ok(userService.withdrawUser(authorization, dto));
  }

}
