package tech.justjava.process_manager.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import tech.justjava.process_manager.account.AuthenticationManager;
import tech.justjava.process_manager.chat.domain.TestMessage;
import tech.justjava.process_manager.chat.service.TestChatService;
import tech.justjava.process_manager.keycloak.UserDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final TestChatService testChatService;
    private final ChatService chatService;
    private final AuthenticationManager authenticationManager;

    @GetMapping("/chat")
    public String chatPage(Model model)
    {
        List<UserDTO> users = chatService.getUsers();
        System.out.println(users);
        model.addAttribute("currentUser",authenticationManager.get("sub"));
        model.addAttribute("users",users);
        return "chat/chat";
    }
    
    // Video call endpoint
    @GetMapping("/api/chat/video-call/user-info")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getVideoCallUserInfo(Authentication authentication) {
        try {
            String currentUserId = getCurrentUserId(authentication);
            String currentUserName = getCurrentUserName(authentication);
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", currentUserId);
            userInfo.put("userName", currentUserName);
            userInfo.put("status", "success");
            
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            System.err.println("Error getting video call user info: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to get user info");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/api/chat/messages/send")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendMessage(
            @RequestParam String senderId,
            @RequestParam String senderName,
            @RequestParam String receiverId,
            @RequestParam String receiverName,
            @RequestParam String content) {

        try {
            TestMessage testMessage = testChatService.sendMessage(senderId, senderName, receiverId, receiverName, content);
            Map<String, Object> response = testChatService.formatMessageForResponse(testMessage);
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to send message");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/api/chat/messages/send-group")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendGroupMessage(
            @RequestParam String senderId,
            @RequestParam String senderName,
            @RequestParam List<String> receiverIds,
            @RequestParam String content) {

        try {
            TestMessage testMessage = testChatService.sendGroupMessage(senderId, senderName, receiverIds, content);
            Map<String, Object> response = testChatService.formatMessageForResponse(testMessage);
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to send group message");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/api/chat/conversations/{userId}")
    @ResponseBody
    public ResponseEntity<List<String>> getUserConversations(@PathVariable String userId) {
        try {
            List<String> conversations = testChatService.getUserConversations(userId);
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    @GetMapping("/api/chat/messages/{conversationId}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getConversationMessages(@PathVariable String conversationId) {
        try {
            List<TestMessage> testMessages = testChatService.getConversationMessages(conversationId);
            List<Map<String, Object>> response = testMessages.stream()
                    .map(testChatService::formatMessageForResponse)
                    .toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage message) {
        System.out.println(" I received ===="+message);
        /***
         *      ///SENDING
         *      Create a Json of this, to this endpoint to send a message
         *     { receiverId : {the recipient's userId},
         *        senderId : {you the logged in user},
         *       content : {message to be sent}
         *      }
         *
         *      ///RECEIVING
         *      When receiving a message, a message of this type would be received
         *      { senderId : {the sender's userId},
         *        receiverId : {the Id of the user receiving the message},
         *        content : {message}
         *      }
         *      All users will be subscribed to
         *          stompClient.subscribe(`/topic/group/` + {their userId}, function (messageOutput) {
         *
         *    You can then append the message to the respective user on the frontend based on the sender's Id
         */
        //String userId = (String) authenticationManager.get("sub");
        String destination = "/topic/group/" + message.getReceiverId();
        //message.setSenderId("449a5325-da3e-4692-93ea-ce8da8346e2f");
        chatService.newMessage(message);
        messagingTemplate.convertAndSend(destination, message);
    }

    private String getCurrentUserName(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
            String firstName = oidcUser.getClaimAsString("given_name");
            String lastName = oidcUser.getClaimAsString("family_name");

            if (firstName != null && lastName != null) {
                return firstName + " " + lastName;
            } else if (firstName != null) {
                return firstName;
            } else {
                return oidcUser.getClaimAsString("preferred_username");
            }
        }
        return "User"; // Fallback for testing
    }
    private String getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
            return oidcUser.getSubject(); // This is the user ID from Keycloak
        }
        return "anonymous"; // Fallback for testing
    }
}