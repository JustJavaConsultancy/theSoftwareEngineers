package tech.justjava.process_manager.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.justjava.process_manager.chat.entity.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {
}