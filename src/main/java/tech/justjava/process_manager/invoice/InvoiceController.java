package tech.justjava.process_manager.invoice;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/invoice")
public class InvoiceController {
    @GetMapping
    public String getInvoices(){
        return "invoice/invoice";
    }
    @GetMapping("/create")
    public String addInvoice(){
        return "invoice/createInvoice";
    }
    @PostMapping("/create-invoice")
    public String getInvoice(
            @RequestParam String clientName,
            @RequestParam String phoneNumber,
            @RequestParam double amount,
            @RequestParam String dueDate,
            @RequestParam(required = false) String description
    ) {
        System.out.println("Client: " + clientName + ", Amount: " + amount + ", Phone Number: " + phoneNumber + ", Due date: " + dueDate + ", Description: " + description);
        return "redirect:/invoice";
    }
}
