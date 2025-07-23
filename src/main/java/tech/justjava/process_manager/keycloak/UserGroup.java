package tech.justjava.process_manager.keycloak;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String  groupId;
    @Column(unique=true)
    private String groupName;
    private String description;
    @Builder.Default
    private Integer members = 0;

    public String getGroupName() {
        if (groupName == null || groupName.isEmpty())
            return groupName;
        groupName = groupName.toLowerCase();
        return groupName.substring(0, 1).toUpperCase() + groupName.substring(1);
    }

    public String getDescription() {
        if (description == null || description.isEmpty())
            return "No description provided";
        return description;
    }

    public Integer getMembers() {
        return members!=null?members:0;
    }
}
