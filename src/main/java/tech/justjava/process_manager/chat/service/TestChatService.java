package tech.justjava.process_manager.chat.service;

import org.springframework.stereotype.Service;
import tech.justjava.process_manager.chat.domain.TestMessage;
import tech.justjava.process_manager.chat.repository.TestMessageRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TestChatService {
    
    private final TestMessageRepository testMessageRepository;
    
    public TestChatService(TestMessageRepository messageRepository) {
        this.testMessageRepository = messageRepository;
    }
    
    public TestMessage sendMessage(String senderId, String senderName, String receiverId, String receiverName, String content) {
        String conversationId = generateConversationId(senderId, receiverId);
        
        TestMessage testMessage = new TestMessage();
        testMessage.setSenderId(senderId);
        testMessage.setSenderName(senderName);
        testMessage.setReceiverId(receiverId);
        testMessage.setReceiverName(receiverName);
        testMessage.setContent(content);
        testMessage.setConversationId(conversationId);
        testMessage.setIsGroupMessage(false);
        testMessage.setTimestamp(LocalDateTime.now());
        
        return testMessageRepository.save(testMessage);
    }
    
    public TestMessage sendGroupMessage(String senderId, String senderName, List<String> receiverIds, String content) {
        String conversationId = generateGroupConversationId(receiverIds);
        
        TestMessage testMessage = new TestMessage();
        testMessage.setSenderId(senderId);
        testMessage.setSenderName(senderName);
        testMessage.setReceiverId(String.join(",", receiverIds));
        testMessage.setReceiverName("Group Chat");
        testMessage.setContent(content);
        testMessage.setConversationId(conversationId);
        testMessage.setIsGroupMessage(true);
        testMessage.setTimestamp(LocalDateTime.now());
        
        return testMessageRepository.save(testMessage);
    }
    
    public List<TestMessage> getConversationMessages(String conversationId) {
        return testMessageRepository.findByConversationIdOrderByTimestamp(conversationId);
    }
    
    public List<String> getUserConversations(String userId) {
        return testMessageRepository.findConversationsByUserId(userId);
    }
    
    public List<TestMessage> getMessagesBetweenUsers(String userId1, String userId2) {
        return testMessageRepository.findMessagesBetweenUsers(userId1, userId2);
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
    
    public Map<String, Object> formatMessageForResponse(TestMessage testMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", testMessage.getId());
        response.put("senderId", testMessage.getSenderId());
        response.put("senderName", testMessage.getSenderName());
        response.put("content", testMessage.getContent());
        response.put("timestamp", testMessage.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        response.put("isGroupMessage", testMessage.getIsGroupMessage());
        return response;
    }
} 