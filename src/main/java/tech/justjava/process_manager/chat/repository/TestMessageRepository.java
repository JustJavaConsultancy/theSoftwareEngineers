package tech.justjava.process_manager.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tech.justjava.process_manager.chat.domain.TestMessage;

import java.util.List;

public interface TestMessageRepository extends JpaRepository<TestMessage, Long> {
    @Query("SELECT m FROM TestMessage m WHERE m.conversationId = ?1 ORDER BY m.timestamp ASC")
    List<TestMessage> findByConversationIdOrderByTimestamp(String conversationId);

    @Query("SELECT DISTINCT m.conversationId FROM TestMessage m WHERE m.senderId = ?1 OR m.receiverId = ?1")
    List<String> findConversationsByUserId(String userId);

    @Query("SELECT m FROM TestMessage m WHERE (m.senderId = ?1 AND m.receiverId = ?2) OR (m.senderId = ?2 AND m.receiverId = ?1) ORDER BY m.timestamp ASC")
    List<TestMessage> findMessagesBetweenUsers(String userId1, String userId2);

    @Query("SELECT m FROM TestMessage m WHERE m.conversationId = ?1 ORDER BY m.timestamp DESC LIMIT 1")
    TestMessage findLastMessageInConversation(String conversationId);

}