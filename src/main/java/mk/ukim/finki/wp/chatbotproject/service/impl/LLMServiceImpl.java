package mk.ukim.finki.wp.chatbotproject.service.impl;

import mk.ukim.finki.wp.chatbotproject.models.Message;
import mk.ukim.finki.wp.chatbotproject.models.Role;
import mk.ukim.finki.wp.chatbotproject.service.KnowledgeTools;
import mk.ukim.finki.wp.chatbotproject.service.LLMService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

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

//    @Override
//    public String generateResponse(List<Message> messages, KnowledgeTools knowledgeTools) {
//        // Build the conversation context from message history
//        String conversationContext = buildConversationContext(messages);
//
//        // System prompt that instructs the AI to use the knowledge tool
//        String systemPrompt = "You are a helpful assistant. You have access to a searchKnowledge tool. ALWAYS call it before answering any factual question. If it returns a result, use that as your answer. If it returns 'No relevant knowledge found.', answer from your own knowledge.";
//
//        // Generate response using ChatClient with knowledge tools
//        return chatClient.prompt()
//                .system(systemPrompt)
//                .user(conversationContext)
//                .tools(knowledgeTools)
//                .call()
//                .content();
//    }
@Override
public String generateResponse(List<Message> messages, KnowledgeTools knowledgeTools) {
    String conversationContext = buildConversationContext(messages);

    // Extract the last user message to search knowledge base
    String lastUserMessage = messages.stream()
            .filter(m -> m.getRole() == Role.USER)
            .reduce((first, second) -> second)
            .map(Message::getContent)
            .orElse("");

    // Search knowledge base ourselves — don't rely on model to call the tool
    String knowledgeResult = knowledgeTools.searchKnowledge(lastUserMessage);

    String systemPrompt;
    if (!knowledgeResult.equals("No relevant knowledge found.")) {
        // Found something — force the model to use it
        systemPrompt = "You are a helpful assistant. " +
                "A human-verified answer exists for this question. " +
                "You MUST respond with exactly this answer, do not add or change anything:\n\n" +
                knowledgeResult;
    } else {
        systemPrompt = "You are a helpful assistant.";
    }

    return chatClient.prompt()
            .system(systemPrompt)
            .user(conversationContext)
            .call()
            .content();
}

//    @Override
//    public String generateResponseStream(List<Message> messages, Consumer<String> onChunk) {
//        // Build the conversation context from message history
//        String conversationContext = buildConversationContext(messages);
//
//        // Generate response with streaming using ChatClient
//        StringBuilder fullResponse = new StringBuilder();
//
//        chatClient.prompt()
//                .user(conversationContext)
//                .stream()
//                .content()
//                .doOnNext(chunk -> {
//                    fullResponse.append(chunk);
//                    onChunk.accept(chunk);
//                })
//                .blockLast();
//
//        return fullResponse.toString();
//    }

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
