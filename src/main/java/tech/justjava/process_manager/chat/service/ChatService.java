package tech.justjava.process_manager.chat.service;

import org.springframework.stereotype.Service;
import tech.justjava.process_manager.chat.domain.Message;
import tech.justjava.process_manager.chat.repository.MessageRepository;
import tech.justjava.process_manager.chat.model.ChatUserDTO;
import tech.justjava.process_manager.chat.model.ChatMessageDTO;
import tech.justjava.process_manager.chat.model.ConversationDTO;
import tech.justjava.process_manager.keycloak.KeycloakService;
import tech.justjava.process_manager.keycloak.UserDTO;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {
    
    private final MessageRepository messageRepository;
    private final KeycloakService keycloakService;
    
    public ChatService(MessageRepository messageRepository, KeycloakService keycloakService) {
        this.messageRepository = messageRepository;
        this.keycloakService = keycloakService;
    }
    
    public List<ChatUserDTO> getUsersForChat() {
        try {
            // First try to get users from groups (existing approach)
            List<UserDTO> users = keycloakService.getUsers();
            
            // If no users from groups, try to get all realm users directly
            if (users.isEmpty()) {
                users = keycloakService.getRealmUsers();
            }
            
            return users.stream()
                    .map(this::convertToChartUserDTO)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            System.err.println("Error fetching users for chat: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    public List<ConversationDTO> getConversationsForChat(String currentUserId) {
        List<String> conversationIds = messageRepository.findConversationsByUserId(currentUserId);
        List<ConversationDTO> conversations = new ArrayList<>();
        
        for (String conversationId : conversationIds) {
            List<Message> messages = messageRepository.findByConversationIdOrderByTimestamp(conversationId);
            if (!messages.isEmpty()) {
                ConversationDTO conversation = buildConversationDTO(conversationId, messages, currentUserId);
                conversations.add(conversation);
            }
        }
        
        // Sort by latest message timestamp
        conversations.sort((c1, c2) -> c2.getCreatedAt().compareTo(c1.getCreatedAt()));
        
        return conversations;
    }
    
    private ChatUserDTO convertToChartUserDTO(UserDTO user) {
        return ChatUserDTO.builder()
                .id(Math.abs((long) user.getId().hashCode())) // Generate stable numeric ID from string ID
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .status(user.getStatus())
                .avatar(getInitials(user.getFirstName(), user.getLastName()))
                .online(null) // Could be enhanced with real online status
                .userGroup(user.getGroup())
                .build();
    }
    
    private String getInitials(String firstName, String lastName) {
        String initials = "";
        if (firstName != null && !firstName.isEmpty()) {
            initials += firstName.charAt(0);
        }
        if (lastName != null && !lastName.isEmpty()) {
            initials += lastName.charAt(0);
        }
        return initials.toUpperCase();
    }
    
    private ConversationDTO buildConversationDTO(String conversationId, List<Message> messages, String currentUserId) {
        List<ChatMessageDTO> chatMessages = messages.stream()
                .map(msg -> convertToChatMessageDTO(msg, currentUserId))
                .collect(Collectors.toList());
        
        Message latestMessage = messages.get(messages.size() - 1);
        boolean isGroup = latestMessage.getIsGroupMessage();
        String title = isGroup ? "Group Chat" : getConversationTitle(conversationId, currentUserId);
        
        return ConversationDTO.builder()
                .id(Math.abs((long) conversationId.hashCode())) // Generate stable numeric ID
                .title(title)
                .group(isGroup)
                .createdAt(messages.get(0).getTimestamp())
                .messages(chatMessages)
                .build();
    }
    
    private ChatMessageDTO convertToChatMessageDTO(Message message, String currentUserId) {
        boolean isFromCurrentUser = message.getSenderId().equals(currentUserId);
        String formattedTime = formatMessageTime(message.getTimestamp());
        
        return ChatMessageDTO.builder()
                .content(message.getContent())
                .sender(isFromCurrentUser)
                .sentAt(formattedTime)
                .build();
    }
    
    private String formatMessageTime(LocalDateTime timestamp) {
        LocalDateTime now = LocalDateTime.now();
        
        if (timestamp.toLocalDate().equals(now.toLocalDate())) {
            return "Today, " + timestamp.format(DateTimeFormatter.ofPattern("h:mm a"));
        } else if (timestamp.toLocalDate().equals(now.toLocalDate().minusDays(1))) {
            return "Yesterday, " + timestamp.format(DateTimeFormatter.ofPattern("h:mm a"));
        } else {
            return timestamp.format(DateTimeFormatter.ofPattern("MMM dd, h:mm a"));
        }
    }
    
    private String getConversationTitle(String conversationId, String currentUserId) {
        // Extract other user ID from conversation ID
        String[] userIds = conversationId.replace("group_", "").split("_");
        for (String userId : userIds) {
            if (!userId.equals(currentUserId)) {
                UserDTO otherUser = keycloakService.getSingleUser(userId);
                if (otherUser != null) {
                    return otherUser.getName();
                }
            }
        }
        return "Unknown User";
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