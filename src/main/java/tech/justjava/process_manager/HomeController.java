package tech.justjava.process_manager;

import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import tech.justjava.process_manager.task.model.TaskStatus;

import java.util.List;


@Controller
public class HomeController {
    private final RuntimeService runtimeService;
    private final TaskService  taskService;
    @Value("${app.processKey}")
    private String  processKey;
    private final HistoryService historyService;

    public HomeController(RuntimeService runtimeService, TaskService taskService, HistoryService historyService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }
    @GetMapping("/dashboard")
    public String dashboard(Model model) {


        long processInstancesCount = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(processKey)
                .active()
                .count();

        List<Task> activeTasks=taskService.createTaskQuery()
                .processDefinitionKey(processKey)
                .active()
                .list();
        System.out.println(activeTasks);
        long activeTasksCount=taskService.createTaskQuery()
                .processDefinitionKey(processKey)
                .active()
                .count();
        long completedTasksCount=historyService.createHistoricTaskInstanceQuery()
                .processDefinitionKey(processKey)
                .finished()
                .orderByHistoricTaskInstanceEndTime().desc()
                .count();
        //model.addAttribute("processInstances", processInstances);
        model.addAttribute("processInstancesCount", processInstancesCount);
        model.addAttribute("activeTasksCount", activeTasksCount);
        model.addAttribute("completedTasksCount", completedTasksCount);
        model.addAttribute("activeTasks", activeTasks);
        return "dashboard";
    }

}
