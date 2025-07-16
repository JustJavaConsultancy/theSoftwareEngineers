package tech.justjava.process_manager.process_instance.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.justjava.process_manager.process.domain.Process;
import tech.justjava.process_manager.process_instance.domain.ProcessInstance;


public interface ProcessInstanceRepository extends JpaRepository<ProcessInstance, Long> {

    ProcessInstance findFirstByProcess(Process process);

}
