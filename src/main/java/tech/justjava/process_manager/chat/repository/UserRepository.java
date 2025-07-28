package tech.justjava.process_manager.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.justjava.process_manager.chat.entity.User;

import java.util.Collection;
import java.util.Set;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUserId(String userId);

    User findByUserId(String userId);

    Set<User> findAllByUserIdIn(Collection<String> userIds);
}