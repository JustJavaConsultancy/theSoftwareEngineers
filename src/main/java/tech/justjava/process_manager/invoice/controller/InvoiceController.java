package tech.justjava.process_manager.invoice.controller;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import tech.justjava.process_manager.account.AuthenticationManager;
import tech.justjava.process_manager.invoice.service.InvoiceService;
import tech.justjava.process_manager.process.service.ProcessService;

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

    public InvoiceController(final AuthenticationManager authenticationManager, final ProcessService processService,
                             final InvoiceService invoiceService){
        this.authenticationManager = authenticationManager;
        this.processService = processService;
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public String getInvoices(Model model){

        List<ProcessInstance> allProcessInstance = processService.getAllProcessInstance("invoicing");
        List<Map<String, Object>> allProcessVar = allProcessInstance.stream()
                .map(processInstance -> {
                    return processInstance.getProcessVariables();
                }).toList();

        Long allPendingInvoiceCount = allProcessVar.stream()
                .filter(invoice -> "approved".equalsIgnoreCase((String) invoice.get("status")))
                .count();

        Long allPaidInvoiceCount = allProcessVar.stream()
                .filter(invoice -> "paid".equalsIgnoreCase((String) invoice.get("status")))
                .count();

        BigDecimal totalInvoiceAmount = allProcessVar.stream()
                .map(invoice -> new BigDecimal(String.valueOf(invoice.get("amount"))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("paidInvoiceCount", allPaidInvoiceCount);
        model.addAttribute("pendingInvoiceCount", allPendingInvoiceCount);
        model.addAttribute("totalAmount", totalInvoiceAmount);
        model.addAttribute("allInvoices", allProcessVar);
        return "invoice/invoice";
    }

    @GetMapping("/create")
    public String addInvoice(Model model){

        model.addAttribute("status", "new");
        return "invoice/createInvoice";
    }
    @PostMapping("/create-invoice")
    public String getInvoice(@RequestParam Map<String, Object> formData, Model model) {
        System.out.println("This is the data submitted" + formData);

        String loginUser = (String) authenticationManager.get("sub");
        ProcessInstance processInstance = processService.startProcess("invoicing",loginUser, formData);
        String processInstanceId = processInstance.getProcessInstanceId();

        formData.put("processId", processInstanceId);
        model.addAttribute("invoiceData", formData);
        return "invoice/invoiceReview";
    }

    @PostMapping("/edit-invoice")
    public String editInvoice(@RequestParam Map<String, Object> editData,Model model){
        System.out.println("This is the edit Data" + editData);
        String processInstanceId = (String) editData.get("processId");

        invoiceService.editInvoiceTask(processInstanceId, editData);

        model.addAttribute("invoiceData", editData);
        return "invoice/invoiceReview";
    }

    @PostMapping("/invoice-review")
    public String getReview(@RequestParam Map<String, Object> reviewData,Model model){
        System.out.println("This is the review Data" + reviewData);
        String processInstanceId = (String) reviewData.get("processId");
        String status = (String) reviewData.get("status");

       invoiceService.seniorReviewTask(processInstanceId, reviewData);

       if (status.equalsIgnoreCase("declined")){
           System.out.println("This task was declined");
           model.addAttribute("status", status);
           model.addAttribute("reviewData", reviewData);
           return "invoice/createInvoice";
       } else {
            return "redirect:/invoice";
       }
    }
}
