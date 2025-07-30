package tech.justjava.process_manager.invoice.controller;

import tech.justjava.process_manager.invoice.service.InvoiceService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import tech.justjava.process_manager.account.AuthenticationManager;
import tech.justjava.process_manager.process.service.ProcessService;
import tech.justjava.process_manager.task.service.TaskService;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final TaskService taskService;
    private final RuntimeService runtimeService;
    private final AuthenticationManager authenticationManager;
    private final ProcessService processService;

    public InvoiceController(final InvoiceService invoiceService,
             final TaskService taskService, final AuthenticationManager authenticationManager,
                             final ProcessService processService, final RuntimeService runtimeService) {
        this.invoiceService = invoiceService;
        this.taskService = taskService;
        this.runtimeService = runtimeService;
        this.authenticationManager = authenticationManager;
        this.processService = processService;
    }

    @GetMapping("/createInvoice")
    public String createInvoice(Model model){
        String loginUser = (String) authenticationManager.get("sub");
        ProcessInstance processInstance = processService.getProcessInstanceByBusinessKey(loginUser);
        System.out.println("This is the processinstance" + processInstance);
        String processInstanceId = processInstance.getProcessInstanceId();
        Task singleTask = taskService.getTaskByInstanceAndDefinitionKey(processInstanceId,
                "FormTask_CreateInvoice");
        Map<String, Object> variables = new HashMap<>();
        variables.put("invoiceStatus", "new");
        System.out.println("This is the create tech.justjava.process_manager.invoice form with variables:::" + variables);
        taskService.completeTask(singleTask.getId(), variables);

        return "process/processInstance";
    };

    @GetMapping("/invoiceReview")
    public String invoiceReview(Model model){
        String loginUser = (String) authenticationManager.get("sub");
        ProcessInstance processInstance = processService.getProcessInstanceByBusinessKey(loginUser);
        String processInstanceId = processInstance.getProcessInstanceId();
        Task singleTask = taskService.getTaskByInstanceAndDefinitionKey(processInstanceId,
                "FormTask_SeniorReview");
        Map<String, Object> variables = new HashMap<>();
        variables.put("invoiceStatus", "new");
        System.out.println("This is the senior review tech.justjava.process_manager.invoice form with variables:::" + variables);
        taskService.completeTask(singleTask.getId(), variables);

        return "process/processInstance";
    }

    @GetMapping("/clientPayment")
    public String clientPayment(Model model){
        String loginUser = (String) authenticationManager.get("sub");
        ProcessInstance processInstance = processService.getProcessInstanceByBusinessKey(loginUser);
        String processInstanceId = processInstance.getProcessInstanceId();
        Task singleTask = taskService.getTaskByInstanceAndDefinitionKey(processInstanceId,
                "FormTask_ClientPayment");
        Map<String, Object> variables = new HashMap<>();
        variables.put("invoiceStatus", "new");
        System.out.println("This is the client tech.justjava.process_manager.payment form with variables:::" + variables);
        taskService.completeTask(singleTask.getId(), variables);

        return "process/processInstance";
    }


}
