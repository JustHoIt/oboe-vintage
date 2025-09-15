package com.oboe.backend.config;

import com.oboe.backend.common.constants.SecurityConstants;
import com.oboe.backend.security.JwtAuthenticationFilter;
import com.oboe.backend.security.OAuth2LoginFailureHandler;
import com.oboe.backend.security.OAuth2LoginSuccessHandler;
import com.oboe.backend.user.service.CustomOAuth2UserService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
@EnableMethodSecurity(prePostEnabled = true) // @PreAuthorize, @PostAuthorize 활성화
@RequiredArgsConstructor
public class SecurityConfig {

  private final CustomOAuth2UserService customOAuth2UserService;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
  private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(authz -> authz
            .requestMatchers("/api/v1/health").permitAll()
            .requestMatchers("/api/v1/db-test/**").permitAll()
            .requestMatchers("/api/v1/users/signup/**").permitAll()
            .requestMatchers("/api/v1/users/login").permitAll()
            .requestMatchers("/api/v1/users/refresh").permitAll()
            .requestMatchers("/api/v1/users/find-id").permitAll()
            .requestMatchers("/api/v1/users/reset-password").permitAll()
            .requestMatchers("/api/v1/message/**").permitAll()
            // OAuth2 관련 엔드포인트 허용
            .requestMatchers("/api/auth/**", "/login/oauth2/**", "/oauth2/**").permitAll()
            // Swagger UI 접근 허용
            .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
            .requestMatchers("/api-docs/**", "/v3/api-docs/**").permitAll()
            .requestMatchers("/actuator/**").permitAll()
            // Product API 권한 설정
            .requestMatchers("/api/products/**").authenticated() // 모든 Product API는 인증 필요
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthenticationFilter,
            org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint((request, response, authException) -> {
              response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              response.setContentType("application/json");
              response.getWriter().write(SecurityConstants.AUTHENTICATION_REQUIRED_MESSAGE);
            })
        )
        .oauth2Login(oauth2 -> oauth2
            .userInfoEndpoint(userInfo -> userInfo
                .userService(customOAuth2UserService)
            )
            .successHandler(oAuth2LoginSuccessHandler)
            .failureHandler(oAuth2LoginFailureHandler)
        );

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    // 개발 환경에서는 localhost 허용, 프로덕션에서는 특정 도메인만 허용
    configuration.setAllowedOriginPatterns(Arrays.asList(SecurityConstants.ALLOWED_ORIGINS));
    configuration.setAllowedMethods(Arrays.asList(SecurityConstants.ALLOWED_METHODS));
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
