package tech.justjava.process_manager.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.justjava.process_manager.chat.dto.ConversationDto;
import tech.justjava.process_manager.chat.entity.Conversation;
import tech.justjava.process_manager.chat.entity.Message;
import tech.justjava.process_manager.chat.entity.User;
import tech.justjava.process_manager.chat.repository.ConversationRepository;
import tech.justjava.process_manager.chat.repository.MessageRepository;
import tech.justjava.process_manager.chat.repository.UserRepository;
import tech.justjava.process_manager.keycloak.UserDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public List<UserDTO> getUsers() {
        return mapUsersToDTO(userRepository.findAll());
    }
@Transactional
    public List<ConversationDto> getConversations(String userId) {
        List<Conversation> conversations = conversationRepository.findAllByMembers_UserId("85c5f3ca-cd54-4484-8efc-669d9e6faf61");
        return mapConversationsToDTO(conversations, userId);
    }

    private List<UserDTO> mapUsersToDTO(List<User> users){
        List<UserDTO> dtos = new ArrayList<>();
        for (User user : users) {
            UserDTO userDTO = new UserDTO();
            userDTO.setUserId(user.getUserId());
            userDTO.setFirstName(user.getFirstName());
            userDTO.setLastName(user.getLastName());
            userDTO.setEmail(user.getEmail());
            userDTO.setStatus(user.getStatus());
            userDTO.setGroup(user.getUserGroup().getGroupName());
            userDTO.setAvatar(user.getAvatar());
            dtos.add(userDTO);
        }
        return dtos;
    }

    @Transactional
    public String createConversation(List<String> conversationIds) {
        Optional<Conversation> conversation1 = conversationRepository.findConversationByExactUserIds(conversationIds, conversationIds.size());
        if (conversation1.isPresent()) {
            return conversation1.get().getId().toString();
        }
        Set<User> users = userRepository.findAllByUserIdIn(conversationIds);
        Conversation conversation = new Conversation();
        if (users.size() > 2) {
            conversation.setGroup(true);
        }
        conversation = conversationRepository.save(conversation);
        conversation.setMembers(users);
        conversation = conversationRepository.save(conversation);
        return conversation.getId().toString();
    }

    @Transactional
    public void newMessage(ChatMessage chatMessage) {
        Optional<Conversation> conversation = conversationRepository.findById(chatMessage.getConversationId());
        User user = userRepository.findByUserId(chatMessage.getSenderId());
        if (conversation.isPresent() && user != null) {
            Message message = new Message();
            message.setConversation(conversation.get());
            message.setSender(user);
            message.setContent(chatMessage.getContent());
            conversation.get().getMessages().add(message);
            user.getMessages().add(message);
            messageRepository.save(message);

        }else {
            throw new RuntimeException("User or Conversation not found");
        }
    }

    private List<ConversationDto> mapConversationsToDTO(List<Conversation> conversations, String userId) {
        List<ConversationDto> dtos = new ArrayList<>();
        for (Conversation conversation : conversations) {
            ConversationDto conversationDto = new ConversationDto();
            conversationDto.setId(conversation.getId());
            conversationDto.setGroup(conversation.getGroup());
            conversationDto.setCreatedAt(conversation.getCreatedAt());
            conversationDto.setMessages(mapMessagesToDTO(conversation.getMessages(), userId));
            if (conversation.getGroup()) {
                conversationDto.setTitle(conversation.getTitle());
            }else {
                conversationDto.setTitle(conversation.getReceiver(userId));
            }
            dtos.add(conversationDto);
        }
        return dtos;
    }

    private List<ConversationDto.MessageDto> mapMessagesToDTO(List<Message> messages, String userId) {
        List<ConversationDto.MessageDto> messageDtos = new ArrayList<>();
        for (Message m : messages) {
            ConversationDto.MessageDto messageDto = new ConversationDto.MessageDto();
            messageDto.setContent(m.getContent());
            messageDto.setSender(m.getSender(userId));
            messageDto.setSentAt(m.getSentAt());
            messageDtos.add(messageDto);
        }
        return messageDtos;
    }

    @Transactional
    public String deleteConversation(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId).orElseThrow(() -> new RuntimeException("Not found"));
        conversation.getMembers().clear();
        conversationRepository.save(conversation);
        conversationRepository.delete(conversation);
        return null;
    }
}
