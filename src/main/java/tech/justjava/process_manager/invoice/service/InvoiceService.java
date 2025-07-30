package tech.justjava.process_manager.invoice.service;

import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import tech.justjava.process_manager.task.service.TaskService;

import java.util.HashMap;
import java.util.Map;


@Service("invoiceService")
public class InvoiceService {

    private final TaskService taskService;

    public InvoiceService(final TaskService taskService){
        this.taskService = taskService;
    }

    public void seniorReviewTask(String processInstanceId, Map<String, Object> variables){
        Task singleTask = taskService.getTaskByInstanceAndDefinitionKey(processInstanceId,
                "FormTask_SeniorReview");
        System.out.println("This is the senior review form with variables:::" + variables);
        taskService.completeTask(singleTask.getId(), variables);
    }

    public void editInvoiceTask(String processInstanceId, Map<String, Object> variables){
        Task singleTask = taskService.getTaskByInstanceAndDefinitionKey(processInstanceId,
                "FormTask_EditInvoice");
        System.out.println("This is the editInvoice task with variables:::" + variables);
        taskService.completeTask(singleTask.getId(), variables);
    }
}
