package com.oboe.backend.common.controller;

import com.oboe.backend.common.dto.ResponseDto;
import com.oboe.backend.common.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File", description = "파일 업로드 관련 API")
public class FileController {

    private final FileUploadService fileUploadService;

    //TODO : S3 저장으로 구현 필요함(상품 업로드 때)
    @Operation(summary = "프로필 이미지 업로드", description = "프로필 이미지를 업로드합니다.")
    @PostMapping(value = "/upload/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<String>> uploadProfileImage(@RequestParam("file") MultipartFile file) {
        log.info("프로필 이미지 업로드 요청 - 파일명: {}", file.getOriginalFilename());
        
        String filePath = fileUploadService.uploadProfileImage(file);
        
        log.info("프로필 이미지 업로드 성공 - 경로: {}", filePath);
        return ResponseEntity.ok(ResponseDto.success(filePath));
    }
}
