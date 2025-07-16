package tech.justjava.process_manager.process_instance.service;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import tech.justjava.process_manager.process.domain.Process;
import tech.justjava.process_manager.process.repos.ProcessRepository;
import tech.justjava.process_manager.process_instance.domain.ProcessInstance;
import tech.justjava.process_manager.process_instance.model.ProcessInstanceDTO;
import tech.justjava.process_manager.process_instance.repos.ProcessInstanceRepository;
import tech.justjava.process_manager.task.domain.Task;
import tech.justjava.process_manager.task.repos.TaskRepository;
import tech.justjava.process_manager.process.service.NotFoundException;
import tech.justjava.process_manager.util.ReferencedWarning;


@Service
public class ProcessInstanceService {

    private final ProcessInstanceRepository processInstanceRepository;
    private final ProcessRepository processRepository;
    private final TaskRepository taskRepository;

    public ProcessInstanceService(final ProcessInstanceRepository processInstanceRepository,
            final ProcessRepository processRepository, final TaskRepository taskRepository) {
        this.processInstanceRepository = processInstanceRepository;
        this.processRepository = processRepository;
        this.taskRepository = taskRepository;
    }

    public List<ProcessInstanceDTO> findAll() {
        final List<ProcessInstance> processInstances = processInstanceRepository.findAll(Sort.by("id"));
        return processInstances.stream()
                .map(processInstance -> mapToDTO(processInstance, new ProcessInstanceDTO()))
                .toList();
    }

    public ProcessInstanceDTO get(final Long id) {
        return processInstanceRepository.findById(id)
                .map(processInstance -> mapToDTO(processInstance, new ProcessInstanceDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final ProcessInstanceDTO processInstanceDTO) {
        final ProcessInstance processInstance = new ProcessInstance();
        mapToEntity(processInstanceDTO, processInstance);
        return processInstanceRepository.save(processInstance).getId();
    }

    public void update(final Long id, final ProcessInstanceDTO processInstanceDTO) {
        final ProcessInstance processInstance = processInstanceRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(processInstanceDTO, processInstance);
        processInstanceRepository.save(processInstance);
    }

    public void delete(final Long id) {
        processInstanceRepository.deleteById(id);
    }

    private ProcessInstanceDTO mapToDTO(final ProcessInstance processInstance,
            final ProcessInstanceDTO processInstanceDTO) {
        processInstanceDTO.setId(processInstance.getId());
        processInstanceDTO.setProcessName(processInstance.getProcessName());
        processInstanceDTO.setBusinessKey(processInstance.getBusinessKey());
        processInstanceDTO.setProcessVariable(processInstance.getProcessVariable());
        processInstanceDTO.setStatus(processInstance.getStatus());
        processInstanceDTO.setProcess(processInstance.getProcess() == null ? null : processInstance.getProcess().getId());
        return processInstanceDTO;
    }

    private ProcessInstance mapToEntity(final ProcessInstanceDTO processInstanceDTO,
            final ProcessInstance processInstance) {
        processInstance.setProcessName(processInstanceDTO.getProcessName());
        processInstance.setBusinessKey(processInstanceDTO.getBusinessKey());
        processInstance.setProcessVariable(processInstanceDTO.getProcessVariable());
        processInstance.setStatus(processInstanceDTO.getStatus());
        final Process process = processInstanceDTO.getProcess() == null ? null : processRepository.findById(processInstanceDTO.getProcess())
                .orElseThrow(() -> new NotFoundException("process not found"));
        processInstance.setProcess(process);
        return processInstance;
    }

    public ReferencedWarning getReferencedWarning(final Long id) {
        final ReferencedWarning referencedWarning = new ReferencedWarning();
        final ProcessInstance processInstance = processInstanceRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        final Task processInstanceTask = taskRepository.findFirstByProcessInstance(processInstance);
        if (processInstanceTask != null) {
            referencedWarning.setKey("processInstance.task.processInstance.referenced");
            referencedWarning.addParam(processInstanceTask.getId());
            return referencedWarning;
        }
        return null;
    }

}
