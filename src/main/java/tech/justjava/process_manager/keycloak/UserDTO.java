package tech.justjava.process_manager.keycloak;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDTO {

    private String firstName;
    private String lastName;
    private String email;
    private String status;
    private String group;

    public String getName() {
        return firstName+" "+lastName;
    }
}
