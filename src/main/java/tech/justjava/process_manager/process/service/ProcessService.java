package tech.justjava.process_manager.process.service;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.justjava.process_manager.file.service.FileDataService;
import tech.justjava.process_manager.process.domain.Process;
import tech.justjava.process_manager.process.model.ProcessDTO;
import tech.justjava.process_manager.process.repos.ProcessRepository;
import tech.justjava.process_manager.process_instance.domain.ProcessInstance;
import tech.justjava.process_manager.process_instance.repos.ProcessInstanceRepository;
import tech.justjava.process_manager.util.ReferencedWarning;


@Service("customProcessService")
@Transactional(rollbackFor = Exception.class)
public class ProcessService {

    private final ProcessRepository processRepository;
    private final FileDataService fileDataService;
    private final ProcessInstanceRepository processInstanceRepository;

    public ProcessService(final ProcessRepository processRepository,
            final FileDataService fileDataService,
            final ProcessInstanceRepository processInstanceRepository) {
        this.processRepository = processRepository;
        this.fileDataService = fileDataService;
        this.processInstanceRepository = processInstanceRepository;
    }

    public List<ProcessDTO> findAll() {
        final List<Process> processes = processRepository.findAll(Sort.by("id"));
        return processes.stream()
                .map(process -> mapToDTO(process, new ProcessDTO()))
                .toList();
    }

    public ProcessDTO get(final Long id) {
        return processRepository.findById(id)
                .map(process -> mapToDTO(process, new ProcessDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final ProcessDTO processDTO) {
        final Process process = new Process();
        mapToEntity(processDTO, process);
        fileDataService.persistUpload(process.getDiagram());
        return processRepository.save(process).getId();
    }

    public void update(final Long id, final ProcessDTO processDTO) {
        final Process process = processRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        fileDataService.handleUpdate(process.getDiagram(), processDTO.getDiagram());
        mapToEntity(processDTO, process);
        processRepository.save(process);
    }

    public void delete(final Long id) {
        final Process process = processRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        fileDataService.removeFileContent(process.getDiagram());
        processRepository.delete(process);
    }

    private ProcessDTO mapToDTO(final Process process, final ProcessDTO processDTO) {
        processDTO.setId(process.getId());
        processDTO.setModelId(process.getModelId());
        processDTO.setProcessName(process.getProcessName());
        processDTO.setDiagram(process.getDiagram());
        return processDTO;
    }

    private Process mapToEntity(final ProcessDTO processDTO, final Process process) {
        process.setModelId(processDTO.getModelId());
        process.setProcessName(processDTO.getProcessName());
        process.setDiagram(processDTO.getDiagram());
        return process;
    }

    public boolean modelIdExists(final String modelId) {
        return processRepository.existsByModelIdIgnoreCase(modelId);
    }

    public boolean processNameExists(final String processName) {
        return processRepository.existsByProcessNameIgnoreCase(processName);
    }

    public ReferencedWarning getReferencedWarning(final Long id) {
        final ReferencedWarning referencedWarning = new ReferencedWarning();
        final Process process = processRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        final ProcessInstance processProcessInstance = processInstanceRepository.findFirstByProcess(process);
        if (processProcessInstance != null) {
            referencedWarning.setKey("process.processInstance.process.referenced");
            referencedWarning.addParam(processProcessInstance.getId());
            return referencedWarning;
        }
        return null;
    }

}
