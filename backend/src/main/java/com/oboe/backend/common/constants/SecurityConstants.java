package com.oboe.backend.common.constants;

/**
 * 보안 관련 상수 정의
 */
public final class SecurityConstants {
    
    private SecurityConstants() {
    }
    
    // JWT 관련 상수
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    
    // CORS 관련 상수
    public static final String[] ALLOWED_ORIGINS = {
        "http://localhost:3000",
        "http://localhost:3001", 
        "http://localhost:5173",
        "https://yourdomain.com"
    };
    
    public static final String[] ALLOWED_METHODS = {
        "GET", "POST", "PUT", "DELETE", "OPTIONS"
    };
    
    // Security Headers 관련 상수
    public static final long HSTS_MAX_AGE_SECONDS = 31536000L; // 1년
    
    // 인증 관련 메시지
    public static final String AUTHENTICATION_REQUIRED_MESSAGE = 
        "{\"code\":401,\"message\":\"인증이 필요합니다.\",\"data\":null,\"success\":false}";
}
