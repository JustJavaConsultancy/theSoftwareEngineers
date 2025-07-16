package tech.justjava.process_manager.process.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;


@Service
public class OpenAIService {
    @Autowired
    ObjectMapper objectMapper;

    private final ChatClient  chatClient;
    private final MessageChatMemoryAdvisor chatMemoryAdvisor;

    public OpenAIService(ChatClient.Builder chatClientBuilder,
                         MessageChatMemoryAdvisor chatMemoryAdvisor) {

        this.chatMemoryAdvisor=chatMemoryAdvisor;
        this.chatClient = chatClientBuilder
                .defaultAdvisors(List.of(chatMemoryAdvisor))
                //.defaultFunctions("ticketFunction")
                .build();

    }

    public String chatWithSystempromptTemplate(String systemPromptTemplate, String userMessage) {
        systemPromptTemplate = systemPromptTemplate.formatted(userMessage);
        System.out.println(" systemPromptTemplate==="+systemPromptTemplate);
        String response = chatClient.prompt(systemPromptTemplate).call().content();
        return response;
    }

}
