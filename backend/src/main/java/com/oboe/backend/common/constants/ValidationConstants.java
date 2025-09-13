package com.oboe.backend.common.constants;

/**
 * 유효성 검증 관련 상수 정의
 */
public final class ValidationConstants {
    
    private ValidationConstants() {
    }
    
    // 파일 업로드 관련 상수
    public static final long MAX_FILE_SIZE = 5242880L; // 5MB
    public static final String[] ALLOWED_IMAGE_TYPES = {
        "image/jpeg", "image/png", "image/gif", "image/webp"
    };
    
    // 비밀번호 관련 상수
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 20;
    
    // 닉네임 관련 상수
    public static final int MIN_NICKNAME_LENGTH = 2;
    public static final int MAX_NICKNAME_LENGTH = 20;
    
    // 휴대폰 번호 관련 상수
    public static final String PHONE_NUMBER_PATTERN = "^01[016789]-\\d{3,4}-\\d{4}$";
    
    // 이메일 관련 상수
    public static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
}
