package com.oboe.backend.common.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/v1")
public class HealthController {

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