package tech.justjava.process_manager.chat;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import tech.justjava.process_manager.chat.domain.Message;
import tech.justjava.process_manager.chat.service.ChatService;

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
} 