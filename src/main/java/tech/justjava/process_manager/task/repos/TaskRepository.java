package tech.justjava.process_manager.task.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.justjava.process_manager.process_instance.domain.ProcessInstance;
import tech.justjava.process_manager.task.domain.Task;


public interface TaskRepository extends JpaRepository<Task, Long> {

    Task findFirstByProcessInstance(ProcessInstance processInstance);

}
