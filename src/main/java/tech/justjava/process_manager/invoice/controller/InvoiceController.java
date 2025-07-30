package tech.justjava.process_manager.invoice.controller;

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

        List<Map<String, Object>> invoices = Arrays.asList(
                Map.of(
                        "invoiceNumber", "INV-00123",
                        "companyName", "Tech Solutions Inc.",
                        "contactPerson", "John Doe",
                        "location", "Silicon Valley, CA",
                        "status", "PAID",
                        "amount", new BigDecimal("2500.00"),
                        "dueDate", LocalDate.of(2024, 8, 15)
                ),
                Map.of(
                        "invoiceNumber", "INV-00122",
                        "companyName", "Innovate Co.",
                        "contactPerson", "Jane Smith",
                        "location", "Boston, MA",
                        "status", "PENDING",
                        "amount", new BigDecimal("1200.00"),
                        "dueDate", LocalDate.of(2024, 8, 10)
                ),
                Map.of(
                        "invoiceNumber", "INV-00121",
                        "companyName", "Digital Crafters",
                        "contactPerson", "Sam Wilson",
                        "location", "Austin, TX",
                        "status", "OVERDUE",
                        "amount", new BigDecimal("3750.00"),
                        "dueDate", LocalDate.of(2024, 7, 25)
                )
        );
        model.addAttribute("invoices", invoices);
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
