package mk.ukim.finki.wp.chatbotproject.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI configuration for Ollama integration.
 */
@Configuration
public class AiConfig {

    /**
     * Create and configure ChatClient bean for Spring AI.
     *
     * @param builder the ChatClient builder
     * @return configured ChatClient instance
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}

