package com.oboe.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oboe.backend.common.dto.ResponseDto;
import com.oboe.backend.common.util.JwtUtil;
import com.oboe.backend.user.dto.LoginResponseDto;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.service.CustomOAuth2UserService.CustomOAuth2User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

  private final JwtUtil jwtUtil;
  private final ObjectMapper objectMapper;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {
    
    log.info("OAuth2 로그인 성공 핸들러 실행");
    
    try {
      // CustomOAuth2User에서 User 정보 추출
      CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
      User user = oauth2User.getUser();
      
      log.info("OAuth2 로그인 성공 - 사용자 ID: {}, 이메일: {}, 제공자: {}", 
          user.getId(), user.getEmail(), user.getSocialProvider());

      // JWT 토큰 생성
      String accessToken = jwtUtil.generateAccessToken(user);
      String refreshToken = jwtUtil.generateRefreshToken(user);
      Long expiresIn = jwtUtil.getAccessTokenExpiration() / 1000; // 초 단위로 변환

      // 응답 DTO 생성
      LoginResponseDto responseDto = LoginResponseDto.from(user, accessToken, refreshToken, expiresIn);
      ResponseDto<LoginResponseDto> result = ResponseDto.success(responseDto);

      // 프론트엔드로 리다이렉트하면서 토큰을 URL 파라미터로 전달
      String redirectUrl = String.format(
        "http://localhost:5173/oauth2/callback?accessToken=%s&refreshToken=%s&expiresIn=%d",
        accessToken, refreshToken, expiresIn
      );
      
      response.sendRedirect(redirectUrl);
      
      log.info("OAuth2 JWT 토큰 발급 완료 - 사용자: {}", user.getEmail());
      
    } catch (Exception e) {
      log.error("OAuth2 로그인 성공 핸들러에서 오류 발생", e);
      
      // 오류 응답
      response.setContentType("application/json;charset=UTF-8");
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      
      ResponseDto<Object> errorResponse = ResponseDto.error(500, "OAuth2 로그인 중 오류가 발생했습니다.");
      String jsonResponse = objectMapper.writeValueAsString(errorResponse);
      response.getWriter().write(jsonResponse);
    }
  }
}
