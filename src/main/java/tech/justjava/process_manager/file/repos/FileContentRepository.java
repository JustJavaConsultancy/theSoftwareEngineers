package tech.justjava.process_manager.file.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.justjava.process_manager.file.domain.FileContent;


public interface FileContentRepository extends JpaRepository<FileContent, String> {
}
