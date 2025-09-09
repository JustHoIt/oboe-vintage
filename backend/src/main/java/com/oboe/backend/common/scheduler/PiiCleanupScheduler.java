package com.oboe.backend.common.scheduler;

import com.oboe.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PiiCleanupScheduler {

  private final UserService userService;

  /**
   * 매일 새벽 2시에 완전 삭제 작업을 실행합니다.
   * 30일 전에 탈퇴한 사용자들을 완전히 삭제합니다.
   */
  @Scheduled(cron = "0 0 2 * * ?")
  public void permanentlyDeleteUsers() {
    log.info("완전 삭제 스케줄러 시작");
    try {
      userService.permanentlyDeleteUsers();
      log.info("완전 삭제 스케줄러 완료");
    } catch (Exception e) {
      log.error("완전 삭제 스케줄러 실행 중 오류 발생", e);
    }
  }
}
