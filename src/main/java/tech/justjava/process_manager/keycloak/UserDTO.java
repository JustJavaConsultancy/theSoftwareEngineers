package tech.justjava.process_manager.keycloak;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDTO {

    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private Boolean status;
    private String group;

    public String getName() {
        return firstName+" "+lastName;
    }
    public String getStatus() {
        return status?"Enabled":"Disabled";
    }
}
