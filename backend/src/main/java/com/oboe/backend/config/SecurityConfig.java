package com.oboe.backend.config;

import com.oboe.backend.user.service.CustomOAuth2UserService;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final CustomOAuth2UserService customOAuth2UserService;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
        .authorizeHttpRequests(authz -> authz
            .requestMatchers("/api/v1/health").permitAll()
            .requestMatchers("/api/v1/db-test/**").permitAll()
            .requestMatchers("/api/v1/users/signup/**").permitAll()
            .requestMatchers("/api/v1/message/**").permitAll()
            // OAuth2 관련 엔드포인트 허용
            .requestMatchers("/api/auth/**", "/api/v1/Oauth/**", "/login/oauth2/**", "/oauth2/**").permitAll()
            // Swagger UI 접근 허용
            .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
            .requestMatchers("/api-docs/**", "/v3/api-docs/**").permitAll()
            .requestMatchers("/actuator/**").permitAll()
            .anyRequest().authenticated()
        )
        .oauth2Login(oauth2 -> oauth2
            .userInfoEndpoint(userInfo -> userInfo
                .userService(customOAuth2UserService)
            )
            //임시 확인용
            .defaultSuccessUrl("http://localhost:5173/user", true)
            .failureUrl("http://localhost:5173/oauth2/error")
        );

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    // 개발 환경에서는 localhost 허용, 프로덕션에서는 특정 도메인만 허용
    configuration.setAllowedOriginPatterns(Arrays.asList(
        "http://localhost:3000",  // 프론트엔드 개발 서버
        "http://localhost:3001",  // 추가 프론트엔드 포트
        "http://localhost:5173",  // Vite 개발 서버
        "https://yourdomain.com"  // 프로덕션 도메인 (실제 도메인으로 변경 필요)
    ));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);
    configuration.setExposedHeaders(Arrays.asList("Authorization"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
