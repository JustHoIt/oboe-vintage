package com.oboe.backend.common.constants;

/**
 * 사용자 관련 상수 정의
 */
public final class UserConstants {
    
    private UserConstants() {
    }
    
    // 토큰 관련 상수
    public static final int TOKEN_EXPIRATION_DIVISOR = 1000;
    
    // PII 삭제 관련 상수
    public static final int PII_DELETION_DAYS = 30;
    public static final int HASH_SUBSTRING_LENGTH = 8;
    
    // Redis 키 접두사
    public static final String SMS_VERIFICATION_PREFIX = "sms_verified:";
    
    // 이메일 익명화 관련 상수
    public static final String DELETED_EMAIL_PREFIX = "deleted_";
    public static final String DELETED_EMAIL_DOMAIN = "@deleted.local";
    public static final String DATE_FORMAT_PATTERN = "yyyyMMdd";
}
