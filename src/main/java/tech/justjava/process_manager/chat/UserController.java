package tech.justjava.process_manager.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.justjava.process_manager.account.AuthenticationManager;
import tech.justjava.process_manager.keycloak.KeycloakService;
import tech.justjava.process_manager.keycloak.UserDTO;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {
    private final KeycloakService keycloakService;
    private final ChatService chatService;
    private final OnlineEventListener onlineEventListener;
    private final AuthenticationManager authenticationManager;

    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getUsers(){
        List<UserDTO> users = chatService.getUsers();
        return ResponseEntity.ok().body(users);
    }

    @PostMapping("/conversations")
    public ResponseEntity<?> createConversation(@RequestParam List<String> conversationIds){
        return ResponseEntity.ok(chatService.createConversation(conversationIds));
    }

    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(/*@PathVariable String userId*/){
        String userId = authenticationManager.get("sub").toString();
        return ResponseEntity.ok(chatService.getConversations(userId));
    }

    @GetMapping("/online-users")
    public ResponseEntity<?> getOnlineUsers(){
        return ResponseEntity.ok(onlineEventListener.getOnlineUsers());
    }
}
