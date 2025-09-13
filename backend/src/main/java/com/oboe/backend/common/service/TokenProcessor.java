package com.oboe.backend.common.service;

import com.oboe.backend.common.exception.CustomException;
import com.oboe.backend.common.exception.ErrorCode;
import com.oboe.backend.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JWT 토큰 처리 공통 로직
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenProcessor {
    
    private final JwtUtil jwtUtil;
    
    /**
     * Bearer 토큰에서 이메일을 추출하고 토큰 유효성을 검증
     * 
     * @param bearerToken Authorization 헤더의 Bearer 토큰
     * @param context 로깅용 컨텍스트
     * @return 추출된 이메일
     */
    public String extractEmailFromBearerToken(String bearerToken, String context) {
        try {
            String token = jwtUtil.removeBearerPrefix(bearerToken);
            validateTokenExpiration(token, context);
            return jwtUtil.getEmailFromToken(token);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("{} 실패 - 토큰에서 이메일 추출 실패: {}", context, e.getMessage());
            throw new CustomException(ErrorCode.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        }
    }
    
    /**
     * 토큰 만료 여부 검증
     * 
     * @param token JWT 토큰
     * @param context 로깅용 컨텍스트
     */
    public void validateTokenExpiration(String token, String context) {
        if (jwtUtil.isTokenExpired(token)) {
            log.warn("{} 실패 - 만료된 토큰 사용 시도", context);
            throw new CustomException(ErrorCode.UNAUTHORIZED, "만료된 토큰입니다.");
        }
    }
    
    /**
     * Refresh Token 유효성 검증
     * 
     * @param refreshToken Refresh Token
     * @param context 로깅용 컨텍스트
     * @return 토큰에서 추출한 이메일
     */
    public String validateRefreshTokenAndExtractEmail(String refreshToken, String context) {
        try {
            String token = jwtUtil.removeBearerPrefix(refreshToken);
            
            // Refresh Token 유효성 검증
            if (!jwtUtil.isRefreshToken(token)) {
                log.warn("{} 실패 - 유효하지 않은 Refresh Token", context);
                throw new CustomException(ErrorCode.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다.");
            }
            
            if (jwtUtil.isTokenExpired(token)) {
                log.warn("{} 실패 - 만료된 Refresh Token", context);
                throw new CustomException(ErrorCode.UNAUTHORIZED, "만료된 Refresh Token입니다.");
            }
            
            return jwtUtil.getEmailFromToken(token);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("{} 중 오류 발생", context, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, context + " 중 오류가 발생했습니다.");
        }
    }
}
