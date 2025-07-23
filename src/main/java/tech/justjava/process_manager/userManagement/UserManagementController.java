package tech.justjava.process_manager.userManagement;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
    public ResponseEntity<Void> createUser(@RequestParam Map<String, String> params){
        keycloakService.createUserInGroup(params);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/createGroup")
    public ResponseEntity<Void> createGroup(@RequestParam Map<String, String> params){
        keycloakService.createGroup(params.get("groupName"), params.get("groupDescription"));
        return ResponseEntity.ok().build();
    }
}
