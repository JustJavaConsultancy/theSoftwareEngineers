package tech.justjava.process_manager.invoice.controller;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import tech.justjava.process_manager.account.AuthenticationManager;
import tech.justjava.process_manager.invoice.service.InvoiceService;
import tech.justjava.process_manager.process.service.ProcessService;
import tech.justjava.process_manager.task.service.TaskService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/invoice")
public class InvoiceController {

    private final AuthenticationManager authenticationManager;
    private final ProcessService processService;
    private final InvoiceService invoiceService;
    private final TaskService taskService;

    public InvoiceController(final AuthenticationManager authenticationManager, final ProcessService processService,
                             final InvoiceService invoiceService, TaskService taskService){
        this.authenticationManager = authenticationManager;
        this.processService = processService;
        this.invoiceService = invoiceService;
        this.taskService = taskService;
    }

    @GetMapping
    public String getInvoices(Model model){
        String loginUser = (String) authenticationManager.get("sub");
        List<ProcessInstance> allProcessInstance = processService.getAllProcessInstance("invoicing", loginUser);
        List<Map<String, Object>> allProcessVar = allProcessInstance.stream()
                .map(processInstance -> {
                    String processId = processInstance.getProcessInstanceId();
                    Map<String, Object> allSingleProcessVar = processInstance.getProcessVariables();
                    allSingleProcessVar.put("processId", processId);
                    return allSingleProcessVar;
                }).toList();

        Long allPendingInvoiceCount = allProcessVar.stream()
                .filter(invoice -> "approved".equalsIgnoreCase((String) invoice.get("status")))
                .count();

        Long allPaidInvoiceCount = allProcessVar.stream()
                .filter(invoice -> "paid".equalsIgnoreCase((String) invoice.get("status")))
                .count();

        BigDecimal totalInvoiceAmount = allProcessVar.stream()
                .filter(invoice -> "paid".equalsIgnoreCase((String) invoice.get("status")))
                .map(invoice -> {
                    String amount = (String) invoice.get("amount");
                    return new BigDecimal(amount != null ? amount : "0");
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

//        System.out.println("This is all Process Var" + allProcessVar);

        model.addAttribute("paidInvoiceCount", allPaidInvoiceCount);
        model.addAttribute("pendingInvoiceCount", allPendingInvoiceCount);
        model.addAttribute("totalAmount", totalInvoiceAmount);
        model.addAttribute("allInvoices", allProcessVar);
        return "invoice/invoice";
    }

    @GetMapping("/view-invoice/{processId}")
    public String getInvoiceDetail(@PathVariable String processId, Model model){
        ProcessInstance singleProcessInstance = processService.getSingleProcessInstance(processId, "invoicing");
        Map<String, Object> processVar = singleProcessInstance.getProcessVariables();

        System.out.println("This is the single process" + processVar);
        model.addAttribute("singleInvoice", processVar);
        return "invoice/invoiceDetail";
    }

    @GetMapping("/create")
    public String addInvoice(Model model){

        model.addAttribute("status", "new");
        return "invoice/createInvoice";
    }
    @PostMapping("/create-invoice")
    public String getInvoice(@RequestParam Map<String, Object> formData, Model model) {
//        System.out.println("This is the data submitted" + formData);

        String loginUser = (String) authenticationManager.get("sub");
        formData.put("status", "new");
        processService.startProcess("invoicing",loginUser, formData);

        model.addAttribute("invoiceData", formData);
        return "redirect:/invoice";
    }

    @GetMapping("/edit-invoice/{id}")
    public String getEditInvoice(@PathVariable String id,Model model){
        Task singleTask = taskService.getTaskByInstanceAndDefinitionKey(id, "FormTask_EditInvoice");

        Map<String, Object> singleTaskVar = singleTask.getProcessVariables();
        singleTaskVar.put("taskId", singleTask.getId());
//        System.out.println("This is the edit Data" + singleTaskVar);

        model.addAttribute("reviewData", singleTaskVar);
        return "invoice/createInvoice";
    }

    @PostMapping("/submit-editInvoice")
    public String submitEditInvoice(@RequestParam Map<String, Object> editData, Model model){
//        System.out.println("This is the edit Data" + editData);
        String taskId = (String) editData.get("taskId");
        editData.put("status", "pending");
        invoiceService.editInvoiceTask(taskId, editData);

        model.addAttribute("invoiceData", editData);
        return "redirect:/invoice";
    }

    @GetMapping("/invoice-review")
    public String getInvoiceReview(Model model){
        List<Task> allPendingReview = taskService.getTaskByAssigneeAndProcessDefinitionKey("manager",
                "invoicing");

        List<HistoricTaskInstance> allApprovedInvoices = taskService.getCompletedTaskByAssigneeAndVariable("manager",
                "invoicing", "status", "approved");

        List<HistoricTaskInstance> allDeclinedInvoices = taskService.getCompletedTaskByAssigneeAndVariable("manager",
                "invoicing", "status", "declined");


//        System.out.println("\n\n This is the historic task " + allApprovedInvoices);

        List<Map<String, Object>> allPendingInvoiceVariables = allPendingReview.stream()
                        .map(invoice -> {
                            Map<String, Object> invoiceVariables = invoice.getProcessVariables();
                            String taskId = invoice.getId();
                            invoiceVariables.put("taskId", taskId);
                            return invoiceVariables;
                        }).toList();

//        System.out.println("This is the pending invoice review" + allPendingInvoiceVariables);

        model.addAttribute("allApprovedCount", allApprovedInvoices.size());
        model.addAttribute("allDeclinedCount", allDeclinedInvoices.size());
        model.addAttribute("allPendingCount", allPendingReview.size());
        model.addAttribute("allPendingInvoice", allPendingInvoiceVariables);
        return "invoice/invoiceReviewHome";
    }

    @GetMapping("/invoice-review/{taskId}")
    public String getInvoiceReviewDetails(@PathVariable String taskId, Model model){
        Task singleTask = taskService.findTaskById(taskId);

        Map<String, Object> singleTaskVar = singleTask.getProcessVariables();

        singleTaskVar.put("taskId", taskId);
//        System.out.println("This is the current single task::" + singleTaskVar);

        model.addAttribute("invoiceData", singleTaskVar);
        return "invoice/invoiceReview";
    }

    @PostMapping("/submit-review")
    public ResponseEntity<Void> submitReview(@RequestParam Map<String, Object> reviewData, Model model){
        System.out.println("This is the review Data" + reviewData);
        String taskId = (String) reviewData.get("taskId");
        String status = (String) reviewData.get("status");

       invoiceService.seniorReviewTask(taskId, reviewData);

       HttpHeaders headers = new HttpHeaders();
       headers.add("HX-Redirect", "/invoice/invoice-review");
       return ResponseEntity.status(HttpStatus.OK).headers(headers).build();
    }
}
