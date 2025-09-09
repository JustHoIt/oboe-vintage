package com.oboe.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "coolsms.apikey=test_api_key",
    "coolsms.apisecret=test_api_secret", 
    "coolsms.fromnumber=01012345678",
    "jwt.secret=test_jwt_secret_key_for_testing_purposes_only_32_chars",
    "jwt.access-token-expiration=86400000",
    "jwt.refresh-token-expiration=604800000"
})
class BackendApplicationTests {

  @Test
  void contextLoads() {
  }

}
