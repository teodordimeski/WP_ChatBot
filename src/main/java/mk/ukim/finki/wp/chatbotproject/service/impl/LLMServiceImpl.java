package mk.ukim.finki.wp.chatbotproject.service.impl;

import mk.ukim.finki.wp.chatbotproject.models.Message;
import mk.ukim.finki.wp.chatbotproject.models.Role;
import mk.ukim.finki.wp.chatbotproject.service.LLMService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of LLMService using Spring AI with Ollama.
 * Generates AI responses based on conversation history.
 */
@Service
public class LLMServiceImpl implements LLMService {

    private final ChatClient chatClient;

    public LLMServiceImpl(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String generateResponse(List<Message> messages) {
        // Build the conversation context from message history
        String conversationContext = buildConversationContext(messages);

        // Generate response using ChatClient (Ollama)
        return chatClient.prompt()
                .user(conversationContext)
                .call()
                .content();
    }

    /**
     * Build a conversation context string from the message history.
     * Format: includes all previous messages in order to provide context to the AI.
     *
     * @param messages the list of messages in the conversation
     * @return formatted conversation context
     */
    private String buildConversationContext(List<Message> messages) {
        StringBuilder contextBuilder = new StringBuilder();

        // Add all messages in order to build context
        for (Message msg : messages) {
            if (msg.getRole() == Role.USER) {
                contextBuilder.append("User: ").append(msg.getContent()).append("\n");
            } else if (msg.getRole() == Role.AI) {
                contextBuilder.append("Assistant: ").append(msg.getContent()).append("\n");
            }
        }

        // Add a prompt for the AI to continue
        contextBuilder.append("Assistant: ");

        return contextBuilder.toString();
    }
}



