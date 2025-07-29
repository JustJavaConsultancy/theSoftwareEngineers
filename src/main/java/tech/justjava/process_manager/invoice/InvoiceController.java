package tech.justjava.process_manager.invoice;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/invoice")
public class InvoiceController {
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
