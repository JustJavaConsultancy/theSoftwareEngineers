package tech.justjava.process_manager;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "home/index";
    }
    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

}
