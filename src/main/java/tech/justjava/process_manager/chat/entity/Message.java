package tech.justjava.process_manager.chat.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

import static tech.justjava.process_manager.util.StringUtils.InstantToStringDate;


@Entity
@Getter
@Setter
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    private String senderId;
    private String receiverName;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Instant sentAt = Instant.now();

    public boolean getSender(String userId) {
        return this.senderId.equals(userId);
    }
    public String getSentAt() {
        return InstantToStringDate(this.sentAt);
    }
}
