package tech.justjava.process_manager.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tech.justjava.process_manager.chat.entity.Conversation;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    @Query("""
  SELECT c FROM Conversation c
  JOIN c.members m
  WHERE m.userId IN :userIds
  GROUP BY c.id
  HAVING COUNT(m.userId) = :size AND COUNT(m.userId) = SIZE(c.members)
""")
    Optional<Conversation> findConversationByExactUserIds(List<String> userIds, int size);

    List<Conversation> findAllByMembers_UserId(String members_userId);
}