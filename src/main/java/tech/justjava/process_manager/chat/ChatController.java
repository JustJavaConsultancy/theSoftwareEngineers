package tech.justjava.process_manager.chat;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import tech.justjava.process_manager.chat.domain.Message;
import tech.justjava.process_manager.chat.service.ChatService;
import tech.justjava.process_manager.chat.model.ChatUserDTO;
import tech.justjava.process_manager.chat.model.ConversationDTO;

import java.util.*;

@Controller
public class ChatController {
    
    private final ChatService chatService;
    
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }
    
    @GetMapping("/chat")
    public String chatPage() {
        return "chat";
    }
    
    // New endpoints for the updated API format
    @GetMapping("/api/chat/conversations")
    @ResponseBody
    public ResponseEntity<List<ConversationDTO>> getConversations(Authentication authentication) {
        try {
            String currentUserId = getCurrentUserId(authentication);
            List<ConversationDTO> conversations = chatService.getConversationsForChat(currentUserId);
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            System.err.println("Error getting conversations: " + e.getMessage());
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }
    
    @GetMapping("/api/chat/users")
    @ResponseBody
    public ResponseEntity<List<ChatUserDTO>> getUsers() {
        try {
            List<ChatUserDTO> users = chatService.getUsersForChat();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            System.err.println("Error getting users: " + e.getMessage());
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }
    
    // Existing endpoints remain the same
    @PostMapping("/api/chat/messages/send")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendMessage(
            @RequestParam String senderId,
            @RequestParam String senderName,
            @RequestParam String receiverId,
            @RequestParam String receiverName,
            @RequestParam String content) {
        
        try {
            Message message = chatService.sendMessage(senderId, senderName, receiverId, receiverName, content);
            Map<String, Object> response = chatService.formatMessageForResponse(message);
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
            Message message = chatService.sendGroupMessage(senderId, senderName, receiverIds, content);
            Map<String, Object> response = chatService.formatMessageForResponse(message);
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
            List<String> conversations = chatService.getUserConversations(userId);
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }
    
    @GetMapping("/api/chat/messages/{conversationId}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getConversationMessages(@PathVariable String conversationId) {
        try {
            List<Message> messages = chatService.getConversationMessages(conversationId);
            List<Map<String, Object>> response = messages.stream()
                    .map(chatService::formatMessageForResponse)
                    .toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }
    
    private String getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
            return oidcUser.getSubject(); // This is the user ID from Keycloak
        }
        return "anonymous"; // Fallback for testing
    }
} 