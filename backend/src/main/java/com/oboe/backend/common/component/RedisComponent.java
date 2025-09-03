package com.oboe.backend.common.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Slf4j
public class RedisComponent {

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  public void setExpiration(String key, Object obj, Duration expiration) {
    log.info("Redis 저장 시작 - 키: {}, 값: {}, 만료시간: {}초", key, obj, expiration.getSeconds());
    
    try {
      // String 값으로 저장 (인증번호는 String이므로)
      redisTemplate.opsForValue().set(key, obj.toString(), expiration);
      
      // 저장 후 TTL 확인
      Long ttl = redisTemplate.getExpire(key);
      log.info("Redis 저장 완료 - 키: {}, TTL: {}초", key, ttl);
      
    } catch (Exception e) {
      log.error("Redis 저장 실패 - 키: {}, 오류: {}", key, e.getMessage(), e);
      throw e;
    }
  }

  public void set(String key, Object obj) {
    log.info("Redis 저장 (만료시간 없음) - 키: {}, 값: {}", key, obj);
    redisTemplate.opsForValue().set(key, obj.toString());
  }

  public Object get(String key) {
    log.info("Redis 조회 - 키: {}", key);
    
    try {
      Object value = redisTemplate.opsForValue().get(key);
      Long ttl = redisTemplate.getExpire(key);
      log.info("Redis 조회 결과 - 키: {}, 값: {}, 남은 TTL: {}초", key, value, ttl);
      return value;
    } catch (Exception e) {
      log.error("Redis 조회 실패 - 키: {}, 오류: {}", key, e.getMessage(), e);
      throw e;
    }
  }

  public boolean delete(String key) {
    log.info("Redis 삭제 - 키: {}", key);
    boolean result = Boolean.TRUE.equals(redisTemplate.delete(key));
    log.info("Redis 삭제 결과 - 키: {}, 성공: {}", key, result);
    return result;
  }

  public boolean hasKey(String key) {
    boolean result = Boolean.TRUE.equals(redisTemplate.hasKey(key));
    log.info("Redis 키 존재 확인 - 키: {}, 존재: {}", key, result);
    return result;
  }

}
