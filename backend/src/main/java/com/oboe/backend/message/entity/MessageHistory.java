package com.oboe.backend.message.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.nurigo.sdk.message.model.Message;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageHistory {

  //고유 식별자
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String recipient;

  @Column(nullable = false)
  private String message;

  @Column(nullable = false)
  private LocalDateTime sendAt;

  @Column(nullable = false)
  private boolean status;

  private String failureReason;

  public static MessageHistory from(Message message){
    return MessageHistory.builder()
        .recipient(message.getFrom())
        .message(message.getText())
        .sendAt(LocalDateTime.now())
        .build();
  }

}
