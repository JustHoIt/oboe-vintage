package com.oboe.backend.common.controller;

import com.oboe.backend.common.dto.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/v1")
@Tag(name = "Health", description = "서버 상태 확인 API")
public class HealthController {

  @Operation(summary = "서버 상태 확인", description = "백엔드 서버의 현재 상태를 확인합니다.")
  @GetMapping("/health")
  public Map<String, Object> health() {
    Map<String, Object> response = new HashMap<>();
    log.info("health API 실행");
    response.put("Status: ", "UP");
    response.put("Timestamp: ", LocalDateTime.now());
    response.put("Message: ", "Backend server is running");
    return response;
  }
}