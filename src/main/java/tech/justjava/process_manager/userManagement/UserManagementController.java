package tech.justjava.process_manager.userManagement;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final UserService userService;
    private final KeycloakService keycloakService;
    @GetMapping
    public String getUsers(Model model){
        List<UserDTO> users = userService.getUsers();
        model.addAttribute("users", users);

      return "userManagement/list";
    }
    @GetMapping("/editUser/{id}")
    public String editUser(@PathVariable String id, Model model) {
        List<UserGroup> userGroup = userService.getUserGroups();
        UserDTO user = userService.getSingleUser(id);
        System.out.println(user);
        model.addAttribute("user", user);
        model.addAttribute("userGroups", userGroup);
        return "/userManagement/editUser";
    }

    @GetMapping("/new")
    public String addUser(Model model) {
        List<UserGroup> userGroup = userService.getUserGroups();
        model.addAttribute("userGroups", userGroup);
        model.addAttribute("status","added");
        return "userManagement/create";
    }
    @GetMapping("/groups")
    public String manageGroups(Model model) {
        List<UserGroup> userGroups = userService.getUserGroups();
        model.addAttribute("userGroups", userGroups);
        return "userManagement/groupManagement";
    }
    @GetMapping("/editGroup/{id}")
    public String editGroup(@PathVariable String id, Model model) {

        System.out.println(" The ID sent ==="+id);
        UserGroup singleUser = userService.getSingleGroup(id);

        System.out.println(" The singleUser===="+singleUser);
        model.addAttribute("singleGroup",singleUser);
        return "userManagement/editGroup :: content";
    }


    @PostMapping("/createUser")
    public String createUser(@RequestParam Map<String, String> params ,Model model){
        keycloakService.createUserInGroup(params);
        model.addAttribute("status","added");
        return "userManagement/userStatus";
    }

    @PostMapping("/createGroup")
    public ResponseEntity<Void> createGroup(@RequestParam Map<String, String> params){
        keycloakService.createGroup(params.get("groupName"), params.get("groupDescription"));
        HttpHeaders headers = new HttpHeaders();
        headers.add("HX-Redirect", "/users/groups");
        return ResponseEntity.status(HttpStatus.OK).headers(headers).build();
    }

    @PostMapping("/editGroup")
    public ResponseEntity<Void> editGroup(@RequestParam Map<String, String> params){
        keycloakService.updateGroup(params);
        HttpHeaders headers = new HttpHeaders();
        headers.add("HX-Redirect", "/users/groups");
        return ResponseEntity.status(HttpStatus.OK).headers(headers).build();
    }
    @PostMapping("/editUser")
    public String editUser(@RequestParam Map<String, String> params, Model model){
        keycloakService.updateUser(params.get("id"), params);
        model.addAttribute("status","edited");
        return "userManagement/userStatus";
    }
    @GetMapping("/deleteUser/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId){
        keycloakService.deleteUser(userId);
        HttpHeaders headers = new HttpHeaders();
        headers.add("HX-Redirect", "/users");
        return ResponseEntity.status(HttpStatus.OK).headers(headers).build();
    }

    @GetMapping("/deleteGroup/{groupId}")
    public ResponseEntity<Void> deleteGroup(@PathVariable String groupId){
        keycloakService.deleteClientGroup(groupId);
        HttpHeaders headers = new HttpHeaders();
        headers.add("HX-Redirect", "/users/groups");
        return ResponseEntity.status(HttpStatus.OK).headers(headers).build();
    }
}
