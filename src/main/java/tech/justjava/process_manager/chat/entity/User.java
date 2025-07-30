package tech.justjava.process_manager.chat.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import tech.justjava.process_manager.keycloak.UserGroup;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    @Column(nullable = false, unique = true)
    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private Boolean status;
    @ManyToOne
    @JoinColumn(name = "user_group_id")
    private UserGroup userGroup;

    @Transient
    private String avatar;
    @Transient
    private Boolean online;

    @ManyToMany(mappedBy = "members", fetch = FetchType.LAZY)
    private List<Conversation> conversations = new ArrayList<>();

    public String getName() {
        return firstName+" "+lastName;
    }
    public String getStatus() {
        return status?"Enabled":"Disabled";
    }

    public String getAvatar() {
        return String.valueOf(this.firstName.charAt(0));
    }
}
