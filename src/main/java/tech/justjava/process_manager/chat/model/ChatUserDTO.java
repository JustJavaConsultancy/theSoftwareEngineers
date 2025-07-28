package tech.justjava.process_manager.chat.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatUserDTO {
    private Long id;
    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String status;
    private String avatar;
    private Boolean online;
    private String userGroup;
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
} 