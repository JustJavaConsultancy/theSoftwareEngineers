package tech.justjava.process_manager.process.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.justjava.process_manager.process.domain.Process;


public interface ProcessRepository extends JpaRepository<Process, Long> {

    boolean existsByModelIdIgnoreCase(String modelId);

    boolean existsByProcessNameIgnoreCase(String processName);

}
