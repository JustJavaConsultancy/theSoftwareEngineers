package tech.justjava.process_manager.task.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tech.justjava.process_manager.process.form.Form;
import tech.justjava.process_manager.process.form.FormService;
import tech.justjava.process_manager.process.service.ProcessServiceAI;
import tech.justjava.process_manager.process.service.TemplateRenderer;
import tech.justjava.process_manager.process_instance.domain.ProcessInstance;
import tech.justjava.process_manager.process_instance.repos.ProcessInstanceRepository;
import tech.justjava.process_manager.task.model.FormField;
import tech.justjava.process_manager.task.model.TaskDTO;
import tech.justjava.process_manager.task.model.TaskStatus;
import tech.justjava.process_manager.task.service.TaskService;
import tech.justjava.process_manager.util.CustomCollectors;
import tech.justjava.process_manager.util.JsonStringFormatter;
import tech.justjava.process_manager.util.WebUtils;


@Controller
@RequestMapping("/tasks")
public class TaskController {


    @Autowired
    ProcessServiceAI  processServiceAI;

    @Autowired
    TemplateRenderer templateRenderer;
    private final TaskService taskService;
    private final FormService formService;
    private final ObjectMapper objectMapper;
    private final ProcessInstanceRepository processInstanceRepository;

    public TaskController(final TaskService taskService, FormService formService, final ObjectMapper objectMapper,
                          final ProcessInstanceRepository processInstanceRepository) {
        this.taskService = taskService;
        this.formService = formService;
        this.objectMapper = objectMapper;
        this.processInstanceRepository = processInstanceRepository;
    }

    @InitBinder
    public void jsonFormatting(final WebDataBinder binder) {
        binder.addCustomFormatter(new JsonStringFormatter<Map<String, String>>(objectMapper) {
        }, "taskVariable");
        binder.addCustomFormatter(new JsonStringFormatter<List<FormField>>(objectMapper) {
        }, "fields");
    }

    @ModelAttribute
    public void prepareContext(final Model model) {
        model.addAttribute("taskStatusValues", TaskStatus.values());
        model.addAttribute("processInstanceValues", processInstanceRepository.findAll(Sort.by("id"))
                .stream()
                .collect(CustomCollectors.toSortedMap(ProcessInstance::getId, ProcessInstance::getProcessName)));
    }

    @GetMapping
    public String list(final Model model) {
        model.addAttribute("tasks", taskService.findActiveflowableTasks());
        return "task/list";
    }

    @GetMapping("/add/{taskId}")
    public String add(@PathVariable("taskId") final String taskId, Model model) {

        Task  task=taskService.findTaskById(taskId);
        //System.out.println(" Process Variable =="+task.getProcessVariables());
        String formThymeleaf=null;
        Optional<Form> form=formService.findByFormCode(task.getTaskDefinitionKey());
        //System.out.println(" The form code==="+task.getTaskDefinitionKey()+"");
        if(form.isPresent()){
            formThymeleaf=form.get().getFormInterface();
            //System.out.println(" formThymeleaf  "+formThymeleaf);
        }else{
            Form newForm=new Form();
            newForm.setFormCode(task.getTaskDefinitionKey());
            newForm.setFormName(task.getName());
            newForm.setFormDetails(task.getDescription());
            formThymeleaf=processServiceAI.generateTaskThymeleafForm(task.getDescription());
            formThymeleaf=formThymeleaf.replace("```","").replace("html","");
            newForm.setFormInterface(formThymeleaf);
            formService.save(newForm);
        }

        Map<String,Object> formData= task.getProcessVariables();
        //System.out.println(" The FormData Here ===="+formData);


        formData.put("id", task.getId());

        List<String> lawyerDocuments = List.of(
                "Letter of Demand",
                "Statement of Claim",
                "Affidavit of Evidence",
                "Preliminary Objections"
        );
        formData.put("lawyerDocuments",lawyerDocuments);
        String formHtml=templateRenderer.render(formThymeleaf,formData);

        model.addAttribute("lawyerDocuments", lawyerDocuments);

        model.addAttribute("formHtml",formHtml);
        model.addAttribute("name",task.getName());
        return "task/add";
    }

    @PostMapping("/complete")
    public String add(@RequestParam Map<String,Object> formData) {
        //formData.put("shortRoute",true);
        System.out.println("1 Here is the Submitted Data Here==="+formData);

        String taskId = (String) formData.get("id");

        System.out.println("2 Here is the Submitted Data Here==="+formData);
        taskService.completeTask(taskId,formData);
        return "task/successTask";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable(name = "id") final String id, final Model model) {
        String taskDocumentation=taskService.getTaskDocumentation(id);

        //System.out.println(" The Task Documentation==="+taskDocumentation);
        String formThymeleaf=processServiceAI.generateThymeleafForm(taskDocumentation);
        formThymeleaf=formThymeleaf.replace("```","").replace("html","");
        Map<String, Object> formData = Map.of("id", id,"email","akinrinde@justjava.com.ng");



        String formHtml=templateRenderer.render(formThymeleaf,formData);

        //System.out.println(" The Form Fragment==="+formHtml);



        model.addAttribute("formData",formData);
        model.addAttribute("id",id);
        model.addAttribute("email","akinrinde@justjava.com.ng");
        model.addAttribute("formHtml",formHtml);

        return "process/form-fragment";

    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable(name = "id") final Long id,
            @ModelAttribute("task") @Valid final TaskDTO taskDTO, final BindingResult bindingResult,
            final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "task/edit";
        }
        taskService.update(id, taskDTO);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("task.update.success"));
        return "redirect:/tasks";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable(name = "id") final Long id,
            final RedirectAttributes redirectAttributes) {
        taskService.delete(id);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_INFO, WebUtils.getMessage("task.delete.success"));
        return "redirect:/tasks";
    }

}
