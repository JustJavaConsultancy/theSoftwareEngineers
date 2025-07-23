package tech.justjava.process_manager.process.form;

import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import tech.justjava.process_manager.process.service.ProcessService;

import tech.justjava.process_manager.process.service.ProcessServiceAI;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/forms")
public class FormController {

    @Value("${app.processKey}")
    private String  processKey;
    private final FormService formService;
    private final ProcessServiceAI processServiceAI;
    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final ProcessService processService;

    public FormController(FormService formService, ProcessServiceAI processServiceAI, RuntimeService runtimeService, RepositoryService repositoryService, ProcessService processService) {
        this.formService = formService;
        this.processServiceAI = processServiceAI;
        this.runtimeService = runtimeService;
        this.repositoryService = repositoryService;
        this.processService = processService;
    }

    @GetMapping
    public String listForms(Model model) {
        model.addAttribute("forms", formService.findAll());
        return "form/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {

        ProcessDefinition processDefinition = repositoryService
                .createProcessDefinitionQuery()
                .processDefinitionKey(processKey)
                .latestVersion()
                .singleResult();
        List<UserTask> userTasks=processService.getProcessUserTasks(processDefinition.getId());
        userTasks.forEach(userTask -> {
            System.out.println(" the usertask id"+userTask.getId()+
                    "  the usertask name"+userTask.getName()+
                    "  the usertask documentation"+userTask.getDocumentation()+
                    "  the usertask assignee"+userTask.getAssignee());
        });
        model.addAttribute("form", new Form());
        model.addAttribute("tasks", userTasks);
        return "form/create";
    }

    @PostMapping
    public String saveForm(@ModelAttribute("form") Form form) {
        formService.save(form);
        return "redirect:/forms";
    }
    @PostMapping("/generate-form")
    public StreamingResponseBody generateForm(
            @RequestParam String selectTask,
            @RequestParam String taskName,
            @RequestParam String taskCode,
            @RequestParam String taskDescription) {

        System.out.println("Form Data Received:");
        System.out.println("Task ID: " + selectTask);
        System.out.println("Form Name: " + taskName);
        System.out.println("Form Code: " + taskCode);
        System.out.println("Description: " + taskDescription);

        String formCode = processServiceAI
                .generateTaskThymeleafForm(taskDescription)
                .replace("```","")
                .replace("html","");

        return outputStream -> {
            try {
                // Stream code character by character
                char[] chars = formCode.toCharArray();
                for (char c : chars) {
                    outputStream.write(new byte[]{(byte) c});
                    outputStream.flush();
                    Thread.sleep(5); // Typing speed
                }

                // Add completion marker
                outputStream.write("\n<!-- GENERATION_COMPLETE -->".getBytes());
                outputStream.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }
    @PostMapping("/save-form")
    public String saveForm(
            @RequestParam String taskName,
            @RequestParam String taskID,
            @RequestParam String taskFormCode,
            @RequestParam String taskFormDescription,
            Model model) {

        // Process and save the form
        System.out.println("Saved form with code: " + taskFormDescription);
        String formCode = processServiceAI
                .generateTaskThymeleafForm(taskFormDescription)
                .replace("```","")
                .replace("html","");
        Form form = new Form();
        form.setFormDetails(taskFormDescription);
        form.setFormName(taskName);
        form.setFormCode(taskID);
        form.setFormInterface(formCode);
        formService.save(form);
        // Add success message
        model.addAttribute("message", "Form saved successfully!");
        return "redirect:/forms"; // Your success page
    }
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        formService.findById(id).ifPresent(form ->
                model.addAttribute("form", form));
        return "form/edit";
    }

    @PostMapping("/update/{id}")
    public String updateForm(@PathVariable Long id,
                             @ModelAttribute Form form) {
        form.setId(id);
        Optional<Form> savedForm=formService.findById(id);
        savedForm.ifPresent(form1 -> {
            if(!form.getFormDetails().equalsIgnoreCase(form1.getFormDetails())){
                String formInterface=processServiceAI
                        .generateTaskThymeleafForm(form.getFormDetails())
                                .replace("```","")
                                .replace("html","");
                form.setFormInterface(formInterface);
            }
        });
        formService.save(form);
        return "redirect:/forms";
    }

    @GetMapping("/delete/{id}")
    public String deleteForm(@PathVariable Long id) {
        formService.deleteById(id);
        return "redirect:/forms";
    }
}
