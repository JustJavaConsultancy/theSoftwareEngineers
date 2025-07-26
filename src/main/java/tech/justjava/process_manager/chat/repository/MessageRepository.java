package tech.justjava.process_manager.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tech.justjava.process_manager.chat.domain.Message;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    @Query("SELECT m FROM Message m WHERE m.conversationId = ?1 ORDER BY m.timestamp ASC")
    List<Message> findByConversationIdOrderByTimestamp(String conversationId);
    
    @Query("SELECT DISTINCT m.conversationId FROM Message m WHERE m.senderId = ?1 OR m.receiverId = ?1")
    List<String> findConversationsByUserId(String userId);
    
    @Query("SELECT m FROM Message m WHERE (m.senderId = ?1 AND m.receiverId = ?2) OR (m.senderId = ?2 AND m.receiverId = ?1) ORDER BY m.timestamp ASC")
    List<Message> findMessagesBetweenUsers(String userId1, String userId2);
    
    @Query("SELECT m FROM Message m WHERE m.conversationId = ?1 ORDER BY m.timestamp DESC LIMIT 1")
    Message findLastMessageInConversation(String conversationId);
} 