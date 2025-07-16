package tech.justjava.process_manager.config;

import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {
    @Bean
    public InMemoryChatMemory chatMemory() {
        return new InMemoryChatMemory(); // Retains conversation history
    }

    @Bean
    public MessageChatMemoryAdvisor chatMemoryAdvisor(InMemoryChatMemory chatMemory) {
        return new MessageChatMemoryAdvisor(chatMemory); // Links memory to the chat flow
    }
}
