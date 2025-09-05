package com.oboe.backend.security;

import com.oboe.backend.common.util.JwtUtil;
import com.oboe.backend.user.entity.User;
import com.oboe.backend.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;
  private final UserRepository userRepository;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
      FilterChain filterChain) throws ServletException, IOException {
    
    try {
      String token = extractTokenFromRequest(request);
      
      if (token != null && jwtUtil.isAccessToken(token)) {
        String email = jwtUtil.getEmailFromToken(token);
        
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
          User user = userRepository.findByEmail(email).orElse(null);
          
          if (user != null && jwtUtil.validateToken(token, email)) {
            // 사용자 상태 확인
            if (isUserValid(user)) {
              setAuthentication(user, token);
              log.debug("JWT 인증 성공 - 사용자: {}", email);
            } else {
              log.warn("JWT 인증 실패 - 비활성 사용자: {}", email);
            }
          } else {
            log.warn("JWT 인증 실패 - 유효하지 않은 토큰 또는 사용자: {}", email);
          }
        }
      }
    } catch (Exception e) {
      log.error("JWT 인증 처리 중 오류 발생", e);
      SecurityContextHolder.clearContext();
    }
    
    filterChain.doFilter(request, response);
  }

  /**
   * 요청에서 JWT 토큰 추출
   */
  private String extractTokenFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
      return jwtUtil.removeBearerPrefix(bearerToken);
    }
    return null;
  }

  /**
   * 사용자 유효성 검증
   */
  private boolean isUserValid(User user) {
    return user.getStatus().name().equals("ACTIVE") && !user.isBanned();
  }

  /**
   * SecurityContext에 인증 정보 설정
   */
  private void setAuthentication(User user, String token) {
    ArrayList<SimpleGrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    
    UsernamePasswordAuthenticationToken authentication = 
        new UsernamePasswordAuthenticationToken(user, token, authorities);
    
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  /**
   * 특정 경로는 JWT 필터를 건너뛰도록 설정
   */
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
    String path = request.getRequestURI();
    
    // JWT 필터를 건너뛸 경로들
    return path.startsWith("/api/v1/health") ||
           path.startsWith("/api/v1/users/signup") ||
           path.startsWith("/api/v1/users/login") ||
           path.startsWith("/api/v1/message") ||
           path.startsWith("/login/oauth2") ||
           path.startsWith("/oauth2") ||
           path.startsWith("/swagger-ui") ||
           path.startsWith("/api-docs") ||
           path.startsWith("/v3/api-docs") ||
           path.startsWith("/actuator");
  }
}
