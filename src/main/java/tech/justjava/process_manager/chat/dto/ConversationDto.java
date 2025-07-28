package tech.justjava.process_manager.chat.dto;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.Data;

import java.io.Serializable;
import java.util.List;


@Data
public class ConversationDto implements Serializable {
    Long id;
    String title;
    Boolean group;
    String createdAt;
    @Embedded
    List<MessageDto> messages;

        @Data
        @Embeddable
    public static class MessageDto implements Serializable {
        String content;
        Boolean sender;
        String sentAt;
    }
}