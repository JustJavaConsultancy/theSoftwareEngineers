package tech.justjava.process_manager.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static tech.justjava.process_manager.util.StringUtils.InstantToStringDate;

@Getter
@Setter
@Entity
@Table(name = "conversations")
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    private String title;
    @Column(name = "isGroup")
    private Boolean group = false;
    private Instant createdAt = Instant.now();

    @ManyToMany
    @JoinTable(name = "conversation_users",
            joinColumns = @JoinColumn(name = "conversation_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> members = new HashSet<>();

    @OneToMany(mappedBy = "conversation")
    @OrderBy("sentAt ASC")
    private List<Message> messages = new ArrayList<>();

    public String getReceiverId(String userId){
        if (members.size() > 2){
            return "";
        }
        return members.stream().map(User::getUserId)
                .filter(userUserId -> ! userUserId.equals(userId))
                .findFirst().orElse(null);
    }
    public String getReceiverName(String userId){
        if (members.size() > 2){
            return "";
        }
        return  members.stream().filter(user -> !user.getUserId().equals(userId))
                .map(User::getName).findFirst().orElse(null);
    }
    public String getCreatedAt() {
        return InstantToStringDate(this.createdAt);
    }


}
