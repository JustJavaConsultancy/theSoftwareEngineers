package tech.justjava.process_manager.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.justjava.process_manager.chat.entity.User;

import java.util.List;

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

    @JsonIgnore
    @OneToMany(mappedBy = "userGroup")
    List<User> users;

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
