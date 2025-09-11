package tech.justjava.process_manager.process.service;

import java.util.*;

import org.flowable.bpmn.model.*;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.ProcessDefinition;
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
import tech.justjava.process_manager.util.StringUtils;

@Service("customProcessService")
@Transactional(rollbackFor = Exception.class)
public class ProcessService {

    private final ProcessRepository processRepository;
    private final FileDataService fileDataService;
    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final ProcessInstanceRepository processInstanceRepository;

    public ProcessService(final ProcessRepository processRepository,
                          final FileDataService fileDataService,
                          RepositoryService repositoryService, RuntimeService runtimeService, final ProcessInstanceRepository processInstanceRepository) {
        this.processRepository = processRepository;
        this.fileDataService = fileDataService;
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.processInstanceRepository = processInstanceRepository;
    }

    public List<ProcessDTO> findAll() {
        final List<Process> processes = processRepository.findAll(Sort.by("id"));
        return processes.stream()
                .map(process -> mapToDTO(process, new ProcessDTO()))
                .toList();
    }

    public List<UserTask> getProcessUserTasks(String processDefinitionID){

        Set<String> visitedDefinitions = new HashSet<>();
        List<UserTask> userTasks = new ArrayList<>();
        collectUserTasksFromDefinition(processDefinitionID, userTasks, visitedDefinitions);
        userTasks.sort(Comparator.comparingInt(UserTask::getXmlRowNumber));

        userTasks.forEach(userTask -> {
            System.out.println(" The Task sorted =="+userTask.getName() +" The rownumber=="+userTask.getXmlRowNumber()+"\n\n");
        });
        return userTasks;

    }
    private void collectUserTasksFromDefinition(String processDefinitionId, List<UserTask> userTasks,
                                                Set<String> visitedDefinitions) {
        // Prevent infinite loop from recursive calls
        if (visitedDefinitions.contains(processDefinitionId)) return;
        visitedDefinitions.add(processDefinitionId);

        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        if (bpmnModel == null) return;

        org.flowable.bpmn.model.Process mainProcess = bpmnModel.getMainProcess();
        collectUserTasks(mainProcess.getFlowElements(), userTasks, visitedDefinitions);

    }
    private void collectUserTasks(Collection<FlowElement> elements, List<UserTask> userTasks, Set<String> visitedDefinitions) {
        for (FlowElement element : elements) {

            if (element instanceof UserTask) {
                userTasks.add((UserTask) element);
            } else if (element instanceof SubProcess) {
                collectUserTasks(((SubProcess) element).getFlowElements(), userTasks, visitedDefinitions);
            } else if (element instanceof CallActivity callActivity) {
                String calledProcessDefinitionKey = callActivity.getCalledElement();
                if (calledProcessDefinitionKey != null) {
                    // Fetch latest version of the called process
                    ProcessDefinition calledProcDef = repositoryService
                            .createProcessDefinitionQuery()
                            .processDefinitionKey(calledProcessDefinitionKey)
                            .latestVersion()
                            .singleResult();
                    if (calledProcDef != null) {
                        collectUserTasksFromDefinition(calledProcDef.getId(), userTasks, visitedDefinitions);
                    }
                }
            }
        }
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

    public org.flowable.engine.runtime.ProcessInstance startProcess(String processKey, String businessKey,
                                                                    Map<String, Object> variables){

        System.out.println("Process Started");

        return runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey(processKey)
                .businessKey(businessKey)
                .variables(variables)
                .start();
    }
    public org.flowable.engine.runtime.ProcessInstance getProcessInstanceByBusinessKey(String businessKey){

        return runtimeService
                .createProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .processDefinitionKey("invoicing")
                .singleResult();
    }

    public List<org.flowable.engine.runtime.ProcessInstance> getAllProcessInstance(String processDefKey, String businessKey){
        return runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(processDefKey)
                .processInstanceBusinessKey(businessKey)
                .includeProcessVariables()
                .active()
                .orderByProcessInstanceId().desc()
                .list();
    }

    public org.flowable.engine.runtime.ProcessInstance getSingleProcessInstance(String processId, String processDefKey){
        return runtimeService.createProcessInstanceQuery()
                .processInstanceId(processId)
                .processDefinitionKey(processDefKey)
                .includeProcessVariables()
                .singleResult();
    }

    public List<ProcessDefinition> getProcessDefinition(){
        return repositoryService
                .createProcessDefinitionQuery()
                .latestVersion()
                .list();
    }

    public List<Map<String, String>> getProcessDefinitionNames(){
        List<ProcessDefinition> processDefinitions = getProcessDefinition();
        return processDefinitions.stream()
                .map(p -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("processKey", p.getKey());
                    map.put("processName", StringUtils.formatCamelCaseText(p.getName()));
                    return map;
                })
                .toList();
    }


}
