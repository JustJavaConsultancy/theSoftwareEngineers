package tech.justjava.process_manager.report;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/report")
public class ReportController {
    @GetMapping
    public String getReports(){
        return "report/report";
    }

}
