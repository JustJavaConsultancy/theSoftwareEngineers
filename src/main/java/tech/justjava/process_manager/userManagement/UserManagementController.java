package tech.justjava.process_manager.userManagement;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/users")
public class UserManagementController {
    @GetMapping
    public String getUsers(){
      return "userManagement/list";
    };

    @GetMapping("/new")
    public String addUser(){
        return "userManagement/create";
    }
    @GetMapping("/groups")
    public String manageGroups(){
        return "userManagement/groupManagement";
    }
}
