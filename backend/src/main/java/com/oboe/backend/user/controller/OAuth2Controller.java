package com.oboe.backend.user.controller;

import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.service.CustomOAuth2UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/Oauth")
@RequiredArgsConstructor
@Tag(name = "OAuth2 인증", description = "OAuth2 소셜 로그인 관련 API")
public class OAuth2Controller {

  @GetMapping("/login/kakao")
  @Operation(summary = "카카오 로그인", description = "카카오 OAuth2 로그인 페이지로 리다이렉트")
  public ResponseEntity<Map<String, String>> kakaoLogin() {
    Map<String, String> response = new HashMap<>();
    response.put("message", "카카오 로그인 페이지로 이동하세요");
    response.put("url", "/oauth2/authorization/kakao");
    return ResponseEntity.ok(response);
  }

  @GetMapping("/login/naver")
  @Operation(summary = "네이버 로그인", description = "네이버 OAuth2 로그인 페이지로 리다이렉트")
  public ResponseEntity<Map<String, String>> naverLogin() {
    Map<String, String> response = new HashMap<>();
    response.put("message", "네이버 로그인 페이지로 이동하세요");
    response.put("url", "/oauth2/authorization/naver");
    return ResponseEntity.ok(response);
  }

  @GetMapping("/user")
  @Operation(summary = "현재 사용자 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다")
  public ResponseEntity<Map<String, Object>> getCurrentUser(
      @AuthenticationPrincipal CustomOAuth2UserService.CustomOAuth2User oauth2User) {

    log.info("사용자 정보 조회 요청 - oauth2User: {}", oauth2User != null ? "존재" : "null");

    if (oauth2User == null) {
      log.warn("인증되지 않은 사용자 - 401 응답");
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("error", "인증되지 않은 사용자입니다");
      return ResponseEntity.status(401).body(errorResponse);
    }

    User user = oauth2User.getUser();
    log.info("사용자 정보 조회 성공 - 사용자 ID: {}, 이메일: {}", user.getId(), user.getEmail());

    Map<String, Object> userInfo = new HashMap<>();
    userInfo.put("id", user.getId());
    userInfo.put("email", user.getEmail());
    userInfo.put("name", user.getName());
    userInfo.put("nickname", user.getNickname());
    userInfo.put("role", user.getRole());
    userInfo.put("status", user.getStatus());
    userInfo.put("socialProvider", user.getSocialProvider());
    userInfo.put("profileImg", user.getProfileImg());
    userInfo.put("lastLoginAt", user.getLastLoginAt());
    userInfo.put("isBanned", user.isBanned());

    return ResponseEntity.ok(userInfo);
  }

  @PostMapping("/logout")
  @Operation(summary = "로그아웃", description = "사용자 로그아웃을 처리합니다")
  public ResponseEntity<Map<String, String>> logout() {
    Map<String, String> response = new HashMap<>();
    response.put("message", "로그아웃되었습니다");
    return ResponseEntity.ok(response);
  }


  @GetMapping("/oauth2/error")
  @Operation(summary = "OAuth2 에러", description = "OAuth2 로그인 실패 시 처리")
  public ResponseEntity<Map<String, Object>> oauth2Error() {
    Map<String, Object> response = new HashMap<>();
    response.put("success", false);
    response.put("message", "OAuth2 로그인 중 오류가 발생했습니다");
    return ResponseEntity.status(400).body(response);
  }

  @GetMapping("/debug/env")
  @Operation(summary = "환경변수 디버그", description = "OAuth2 환경변수 설정 상태 확인")
  public ResponseEntity<Map<String, Object>> debugEnvironment() {
    Map<String, Object> response = new HashMap<>();
    
    // 환경변수 확인 (보안을 위해 일부만 표시)
    String kakaoClientId = System.getenv("kakao_client_id");
    String naverClientId = System.getenv("naver_client_id");
    
    response.put("kakao_client_id_set", kakaoClientId != null && !kakaoClientId.isEmpty());
    response.put("naver_client_id_set", naverClientId != null && !naverClientId.isEmpty());
    response.put("kakao_client_id_length", kakaoClientId != null ? kakaoClientId.length() : 0);
    response.put("naver_client_id_length", naverClientId != null ? naverClientId.length() : 0);
    
    return ResponseEntity.ok(response);
  }
}
