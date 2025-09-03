package com.oboe.backend.message.repository;

import com.oboe.backend.message.entity.MessageHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageHistoryRepository extends JpaRepository<MessageHistory, Long> {

}