package tech.justjava.process_manager.userManagement;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import tech.justjava.process_manager.keycloak.KeycloakService;
import tech.justjava.process_manager.keycloak.UserDTO;
import tech.justjava.process_manager.keycloak.UserGroup;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserManagementController {

    private final KeycloakService keycloakService;
    @GetMapping
    public String getUsers(Model model){
        List<UserDTO> users = keycloakService.getUsers();
        model.addAttribute("users", users);

      return "userManagement/list";
    }

    @GetMapping("/new")
    public String addUser(Model model) {
        List<UserGroup> userGroup = keycloakService.getUserGroups();
        model.addAttribute("userGroups", userGroup);

        return "userManagement/create";
    }
    @GetMapping("/groups")
    public String manageGroups(Model model){
        List<UserGroup> userGroup = keycloakService.getUserGroups();
        model.addAttribute("userGroups", userGroup);
        return "userManagement/groupManagement";
    }

    @PostMapping("/createUser")
    public String createUser(@RequestParam Map<String, String> params){
        keycloakService.createUserInGroup(params);
        return "redirect:/users";
    }

    @PostMapping("/createGroup")
    public String createGroup(@RequestParam Map<String, String> params){
        keycloakService.createGroup(params.get("groupName"), params.get("groupDescription"));
        return "redirect:/groups";
    }

    @PostMapping("/editGroup")
    public String editGroup(@RequestParam Map<String, String> params){
        keycloakService.updateGroup(params);
        return "redirect:/groups";
    }
    @PostMapping("/editUser")
    public String editUser(@RequestParam Map<String, String> params){
        keycloakService.updateUser(params.get("id"), params);
        return "redirect:/users";
    }
    @GetMapping("/deleteUser/{userId}")
    public String deleteUser(@PathVariable String userId){
        keycloakService.deleteUser(userId);
        return "redirect:/users";
    }

    @GetMapping("/deleteGroup/{groupId}")
    public String deleteGroup(@PathVariable String groupId){
        keycloakService.deleteGroup(groupId);
        return "redirect:/groups";
    }
}
