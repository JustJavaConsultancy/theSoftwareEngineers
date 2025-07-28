package tech.justjava.process_manager.chat.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDTO {
    private String content;
    private Boolean sender; // true if sent by current user, false if received
    private String sentAt; // formatted time string like "Today, 7:34 PM"
} 