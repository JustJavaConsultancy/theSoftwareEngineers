package tech.justjava.process_manager.chat.service;

import org.springframework.stereotype.Service;
import tech.justjava.process_manager.chat.domain.Message;
import tech.justjava.process_manager.chat.repository.MessageRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ChatService {
    
    private final MessageRepository messageRepository;
    
    public ChatService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }
    
    public Message sendMessage(String senderId, String senderName, String receiverId, String receiverName, String content) {
        String conversationId = generateConversationId(senderId, receiverId);
        
        Message message = new Message();
        message.setSenderId(senderId);
        message.setSenderName(senderName);
        message.setReceiverId(receiverId);
        message.setReceiverName(receiverName);
        message.setContent(content);
        message.setConversationId(conversationId);
        message.setIsGroupMessage(false);
        message.setTimestamp(LocalDateTime.now());
        
        return messageRepository.save(message);
    }
    
    public Message sendGroupMessage(String senderId, String senderName, List<String> receiverIds, String content) {
        String conversationId = generateGroupConversationId(receiverIds);
        
        Message message = new Message();
        message.setSenderId(senderId);
        message.setSenderName(senderName);
        message.setReceiverId(String.join(",", receiverIds));
        message.setReceiverName("Group Chat");
        message.setContent(content);
        message.setConversationId(conversationId);
        message.setIsGroupMessage(true);
        message.setTimestamp(LocalDateTime.now());
        
        return messageRepository.save(message);
    }
    
    public List<Message> getConversationMessages(String conversationId) {
        return messageRepository.findByConversationIdOrderByTimestamp(conversationId);
    }
    
    public List<String> getUserConversations(String userId) {
        return messageRepository.findConversationsByUserId(userId);
    }
    
    public List<Message> getMessagesBetweenUsers(String userId1, String userId2) {
        return messageRepository.findMessagesBetweenUsers(userId1, userId2);
    }
    
    private String generateConversationId(String userId1, String userId2) {
        List<String> users = Arrays.asList(userId1, userId2);
        users.sort(String::compareTo);
        return String.join("_", users);
    }
    
    private String generateGroupConversationId(List<String> userIds) {
        List<String> sortedIds = new ArrayList<>(userIds);
        sortedIds.sort(String::compareTo);
        return "group_" + String.join("_", sortedIds);
    }
    
    public Map<String, Object> formatMessageForResponse(Message message) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", message.getId());
        response.put("senderId", message.getSenderId());
        response.put("senderName", message.getSenderName());
        response.put("content", message.getContent());
        response.put("timestamp", message.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        response.put("isGroupMessage", message.getIsGroupMessage());
        return response;
    }
} 