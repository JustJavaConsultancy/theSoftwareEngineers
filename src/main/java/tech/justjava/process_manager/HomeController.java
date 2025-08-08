package tech.justjava.process_manager;

import jakarta.servlet.http.HttpServletRequest;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import tech.justjava.process_manager.account.AuthenticationManager;
import tech.justjava.process_manager.process.service.ProcessService;
import tech.justjava.process_manager.support.SupportFeignClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    private final RuntimeService runtimeService;
    private final tech.justjava.process_manager.task.service.TaskService taskService;
    private final TaskService flowableTaskService;
    private final HistoryService historyService;
    private final SupportFeignClient supportFeignClient;
    private final AuthenticationManager authenticationManager;
    private final ProcessService processService;

    @Value("${app.processKey}")
    private String processKey;

    public HomeController(
            RuntimeService runtimeService,
            tech.justjava.process_manager.task.service.TaskService taskService,
            TaskService flowableTaskService,
            HistoryService historyService,
            SupportFeignClient supportFeignClient,
            AuthenticationManager authenticationManager,
            ProcessService processService
    ) {
        this.runtimeService = runtimeService;
        this.flowableTaskService = flowableTaskService;
        this.taskService = taskService;
        this.historyService = historyService;
        this.supportFeignClient = supportFeignClient;
        this.authenticationManager = authenticationManager;
        this.processService = processService;
    }

    @GetMapping("/")
    public String index(HttpServletRequest request, Model model) {

        if(authenticationManager.isAdmin()){
            request.getSession(true).setAttribute("isAdmin", true);
        }
        List<Map<String, String>> processNames = processService.getProcessDefinitionNames();

        if(authenticationManager.isManager()){
            request.getSession(true).setAttribute("isManager", true);
        }

        request.getSession(true).setAttribute("processNames", processNames);
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long processInstancesCount = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(processKey)
                .active()
                .count();

        long completedProcessCount= historyService.createHistoricProcessInstanceQuery()
                .finished() // Only completed
                .orderByProcessInstanceEndTime()
                .desc() // Sort by end time
                .count();

        List<Task> activeTasks = flowableTaskService.createTaskQuery()
                .processDefinitionKey(processKey)
                .active()
                .list();

        long activeTasksCount = activeTasks.size();

        long completedTasksCount = historyService.createHistoricTaskInstanceQuery()
                .processDefinitionKey(processKey)
                .finished()
                .orderByHistoricTaskInstanceEndTime()
                .desc()
                .count();

        List<Map<String, String>> processNames = processService.getProcessDefinitionNames();
        model.addAttribute("processNames", processNames);
        model.addAttribute("processInstancesCount", processInstancesCount);
        model.addAttribute("activeTasksCount", activeTasksCount);
        model.addAttribute("completedTasksCount", completedTasksCount);
        model.addAttribute("completedProcessCount", completedProcessCount);
        model.addAttribute("activeTasks", activeTasks);

        return "dashboard";
    }

//    sample customer
    @GetMapping("/support")
    public String supportHandler(Model model){
        String loginUser = (String) authenticationManager.get("sub");
        Map<String, Object> formData = new HashMap<>();
        ProcessInstance processInstance = processService.startProcess("customerSupport",loginUser, formData);

        formData.put("ticketStatus", "open");
        formData.put("issueHandle", true);
        String processInstanceId = processInstance.getProcessInstanceId();
        Task handleTask = taskService.getTaskByInstanceAndDefinitionKey(processInstanceId,
                "FormTask_HandleIssue");
        System.out.println("This is the current task " + handleTask);
        taskService.completeTask(handleTask.getId(), formData);


//        Task reAssignTask = taskService.getTaskByInstanceAndDefinitionKey(processInstanceId,
//                "FormTask_ReAssign");
//        System.out.println("This is the reassign task" + reAssignTask);
//        taskService.completeTask(reAssignTask.getId(), formData);
//
//        formData.put("issueHandle", true);
//        Task handleTask_2 = taskService.getTaskByInstanceAndDefinitionKey(processInstanceId,
//                "FormTask_HandleIssue");
//
//        System.out.println("This is the handle task " + handleTask_2);
//        taskService.completeTask(handleTask_2.getId(), formData);

        return "redirect:/dashboard";
    }


    // === SUPPORT CHAT BACKEND ===
    @PostMapping("/api/chat/send")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleChatMessage(@RequestParam("message") String message) {
        Map<String, Object> response = new HashMap<>();

        try {


            // Send it using Feign client
            String aiResponse = supportFeignClient.postAiMessage(message);
            response.put("status", "success");
            response.put("response", aiResponse);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace(); // log for debugging
            response.put("status", "error");
            response.put("response", "Sorry, something went wrong.");
            return ResponseEntity.status(500).body(response);
        }
    }
}
