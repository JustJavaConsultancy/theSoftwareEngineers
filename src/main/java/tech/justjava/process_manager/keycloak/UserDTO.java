package tech.justjava.process_manager.keycloak;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDTO {

    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String status;
    private String group;

    private Boolean online;
    private String avatar;

    public String getName() {
        return firstName+" "+lastName;
    }

}
