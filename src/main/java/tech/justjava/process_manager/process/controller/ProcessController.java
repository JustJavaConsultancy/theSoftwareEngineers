package tech.justjava.process_manager.process.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tech.justjava.process_manager.file.model.FileData;
import tech.justjava.process_manager.file.service.FileDataService;
import tech.justjava.process_manager.process.form.Form;
import tech.justjava.process_manager.process.form.FormService;
import tech.justjava.process_manager.process.model.ProcessDTO;
import tech.justjava.process_manager.process.service.ProcessService;
import tech.justjava.process_manager.process.service.ProcessServiceAI;
import tech.justjava.process_manager.process.service.TemplateRenderer;
import tech.justjava.process_manager.util.JsonStringFormatter;
import tech.justjava.process_manager.util.ReferencedWarning;
import tech.justjava.process_manager.util.WebUtils;

import java.util.*;


@Controller
@RequestMapping("/processes")
public class ProcessController {

    @Value("${app.processKey}")
    private String  processKey;
    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    TemplateRenderer templateRenderer;


    private final RuntimeService  runtimeService;
    private final HistoryService historyService;
    private final ProcessServiceAI  processServiceAI;

    private final ProcessService processService;
    private final ObjectMapper objectMapper;
    private final FileDataService fileDataService;
    private final FormService formService;

    public ProcessController(RuntimeService runtimeService, HistoryService historyService, ProcessServiceAI processServiceAI, final ProcessService processService, final ObjectMapper objectMapper,
                             final FileDataService fileDataService, FormService formService) {
        this.runtimeService = runtimeService;
        this.historyService = historyService;
        this.processServiceAI = processServiceAI;
        this.processService = processService;
        this.objectMapper = objectMapper;
        this.fileDataService = fileDataService;
        this.formService = formService;
    }

    @InitBinder
    public void jsonFormatting(final WebDataBinder binder) {
        binder.addCustomFormatter(new JsonStringFormatter<FileData>(objectMapper) {
        }, "diagram");
    }

    @GetMapping
    public String list(final Model model) {
        //System.out.println(" I'm in the Process List....");
        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(processKey)
                .includeProcessVariables()
                .active()
                .list();
processInstances.forEach(processInstance -> {
    System.out.println(" The Process variable here==="+processInstance.getProcessVariables());
});
        model.addAttribute("processes", processInstances);

        return "process/processInstance";
    }

    @GetMapping("/add")
    public String add(@ModelAttribute("process") final ProcessDTO processDTO) {
        return "process/add";
    }

    @PostMapping("/add")
    public String add(@ModelAttribute("process") @Valid final ProcessDTO processDTO,
            final BindingResult bindingResult, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "process/add";
        }
        processService.create(processDTO);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("process.create.success"));
        return "redirect:/processes";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable(name = "id") final Long id, final Model model) {
        model.addAttribute("process", processService.get(id));
        model.addAttribute("withDownloads", true);
        return "process/edit";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable(name = "id") final Long id,
            @ModelAttribute("process") @Valid final ProcessDTO processDTO,
            final BindingResult bindingResult, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "process/edit";
        }
        processService.update(id, processDTO);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("process.update.success"));
        return "redirect:/processes";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable(name = "id") final Long id,
            final RedirectAttributes redirectAttributes) {
        final ReferencedWarning referencedWarning = processService.getReferencedWarning(id);
        if (referencedWarning != null) {
            redirectAttributes.addFlashAttribute(WebUtils.MSG_ERROR,
                    WebUtils.getMessage(referencedWarning.getKey(), referencedWarning.getParams().toArray()));
        } else {
            processService.delete(id);
            redirectAttributes.addFlashAttribute(WebUtils.MSG_INFO, WebUtils.getMessage("process.delete.success"));
        }
        return "redirect:/processes";
    }

    @GetMapping("/{id}/diagram/{filename}")
    public ResponseEntity<InputStreamResource> downloadDiagram(
            @PathVariable(name = "id") final Long id) {
        final ProcessDTO processDTO = processService.get(id);
        return fileDataService.provideDownload(processDTO.getDiagram());
    }
    @GetMapping("/newProcess")
    public String startForm(Model model) {
        ProcessDefinition processDefinition = repositoryService
                .createProcessDefinitionQuery()
                .processDefinitionKey(processKey)
                .latestVersion()
                .singleResult();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        org.flowable.bpmn.model.Process process = bpmnModel.getMainProcess();
        //System.out.println(" process name===="+process.getName());
        //System.out.println(" process documentation ===="+process.getDocumentation());
        System.out.println(" process name===="+process.getName());
        System.out.println(" process id===="+process.getId());
        String userPrompt= process.getDocumentation();
        System.out.println(" process documentation ===="+userPrompt);
        String formThymeleaf=null;
        Optional<Form> form=formService.findByFormCode(processKey);
        if(form.isPresent()){
            formThymeleaf=form.get().getFormInterface();
        }else{
            Form newForm=new Form();
            newForm.setFormCode(processKey);
            newForm.setFormName(process.getName());
            newForm.setFormDetails(process.getDocumentation());
            formThymeleaf=processServiceAI.generateThymeleafForm(userPrompt);
            formThymeleaf=formThymeleaf.replace("```","").replace("html","");
            newForm.setFormInterface(formThymeleaf);
            formService.save(newForm);
        }


        Map<String, Object> formData = Map.of("id", processDefinition.getId(),"email","akinrinde@justjava.com.ng");



        String formHtml=templateRenderer.render(formThymeleaf,formData);

        //System.out.println(" The Form Fragment==="+formHtml);



        model.addAttribute("formData",formData);
/*        model.addAttribute("id",id);
        model.addAttribute("email","akinrinde@justjava.com.ng");*/
        model.addAttribute("formHtml",formHtml);

        return "process/form-fragment";
    }
    @PostMapping("/start")
    public String handleFormSubmit(@RequestParam Map<String,Object> formData) {
        //System.out.println(" The Form Data==="+formData);

        ProcessInstance processInstance=
                runtimeService.startProcessInstanceById(formData.get("id").toString(),formData);

        System.out.println("  The Process Instance Variables ==="+processInstance.getProcessVariables());
        return "process/success";
    }
    @GetMapping("/startProcess")
    public String startProcess() {

        return "process/startProcess";
    }
    @GetMapping("/processInstance/{processInstanceId}")
    public String processInstanceDetail(@PathVariable(name = "processInstanceId")
                                        final String processInstanceId, Model model) {
        org.flowable.engine.runtime.ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .includeProcessVariables()
                .singleResult();
        String currentTask= (String) processInstance.getProcessVariables().get("currentTask");

        System.out.println(" processInstance.getProcessDefinitionId()===="+processInstance.getProcessDefinitionId());
        List<UserTask> userTasks = processService.getProcessUserTasks(processInstance.getProcessDefinitionId());



        model.addAttribute("tasks", userTasks);
        model.addAttribute("processId", processInstance.getId());
        model.addAttribute("processName", processInstance.getProcessDefinitionName());
        model.addAttribute("currentTask", currentTask);
        return "process/processModal :: content";
    }
}
