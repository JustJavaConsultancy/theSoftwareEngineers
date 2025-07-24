package tech.justjava.process_manager.keycloak;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {
    UserGroup findByGroupNameIgnoreCase(String groupName);

    UserGroup findByGroupId(String groupId);

    void deleteByGroupId(String groupId);
}