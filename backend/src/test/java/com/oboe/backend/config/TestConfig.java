package com.oboe.backend.config;

import com.oboe.backend.common.service.TokenProcessor;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
@EnableWebSecurity
public class TestConfig {

    /**
     * 테스트용 TokenProcessor Mock Bean
     */
    @Bean
    @Primary
    public TokenProcessor tokenProcessor() {
        return Mockito.mock(TokenProcessor.class);
    }

    /**
     * 테스트용 Security 설정 - 모든 요청 허용
     */
    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authz -> authz.anyRequest().permitAll())
            .build();
    }
}
