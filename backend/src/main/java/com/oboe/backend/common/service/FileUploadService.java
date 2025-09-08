package com.oboe.backend.common.service;

import com.oboe.backend.common.exception.CustomException;
import com.oboe.backend.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileUploadService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.max-size:5242880}") // 5MB
    private long maxFileSize;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
        "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    /**
     * 프로필 이미지 업로드
     */
    public String uploadProfileImage(MultipartFile file) {
        log.info("프로필 이미지 업로드 시작 - 파일명: {}, 크기: {} bytes", 
                file.getOriginalFilename(), file.getSize());

        // 파일 유효성 검증
        validateFile(file);

        try {
            // 업로드 디렉토리 생성
            Path uploadPath = Paths.get(uploadDir, "profiles");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 고유한 파일명 생성
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString() + extension;

            // 파일 저장
            Path targetPath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 상대 경로 반환 (프론트엔드에서 접근 가능한 URL)
            String relativePath = "profiles/" + uniqueFilename;
            
            log.info("프로필 이미지 업로드 성공 - 저장 경로: {}", relativePath);
            return relativePath;

        } catch (IOException e) {
            log.error("프로필 이미지 업로드 실패", e);
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED, "파일 업로드에 실패했습니다.");
        }
    }

    /**
     * 파일 삭제
     */
    public void deleteFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return;
        }

        try {
            Path path = Paths.get(uploadDir, filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                log.info("파일 삭제 성공 - 경로: {}", filePath);
            }
        } catch (IOException e) {
            log.error("파일 삭제 실패 - 경로: {}", filePath, e);
        }
    }

    /**
     * 파일 유효성 검증
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "파일이 선택되지 않았습니다.");
        }

        // 파일 크기 검증
        if (file.getSize() > maxFileSize) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, 
                "파일 크기가 너무 큽니다. 최대 " + (maxFileSize / 1024 / 1024) + "MB까지 업로드 가능합니다.");
        }

        // 파일 타입 검증
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new CustomException(ErrorCode.INVALID_FILE_FORMAT, 
                "지원하지 않는 파일 형식입니다. JPG, PNG, GIF, WEBP 파일만 업로드 가능합니다.");
        }

        // 파일명 검증
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 파일명입니다.");
        }
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
