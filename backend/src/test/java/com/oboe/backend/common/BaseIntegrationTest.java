package com.oboe.backend.common;

import com.oboe.backend.config.TestConfig;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통합 테스트를 위한 공통 Base 클래스
 * 모든 통합 테스트는 이 클래스를 상속받아 사용
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestConfig.class)
public abstract class BaseIntegrationTest {
    // 공통 설정만 포함
    // 구체적인 테스트는 상속받는 클래스에서 구현
}
