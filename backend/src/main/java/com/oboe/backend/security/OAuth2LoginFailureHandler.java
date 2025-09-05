package com.oboe.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oboe.backend.common.dto.ResponseDto;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

  private final ObjectMapper objectMapper;

  @Override
  public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException exception) throws IOException, ServletException {
    
    log.error("OAuth2 로그인 실패: {}", exception.getMessage(), exception);
    
    // 프론트엔드로 리다이렉트하면서 에러 메시지 전달
    String redirectUrl = String.format(
      "http://localhost:5173/login?error=oauth2_failed&message=%s",
      java.net.URLEncoder.encode(exception.getMessage(), "UTF-8")
    );
    
    response.sendRedirect(redirectUrl);
  }
}
