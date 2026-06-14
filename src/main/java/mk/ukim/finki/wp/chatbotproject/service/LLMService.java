package mk.ukim.finki.wp.chatbotproject.service;

import mk.ukim.finki.wp.chatbotproject.models.Message;

import java.util.List;

/**
 * Service interface for LLM (Large Language Model) operations.
 * Uses Spring AI with Ollama to generate AI responses.
 */
public interface LLMService {

    /**
     * Generate an AI response based on the conversation history.
     * Converts message history into a conversation context and sends it to the LLM.
     *
     * @param messages list of messages representing the conversation history
     * @return the generated AI response as a string
     */
    String generateResponse(List<Message> messages);

    /**
     * Generate an AI response with knowledge tools support.
     * Converts message history into a conversation context, registers the knowledge tools,
     * and sends it to the LLM. The LLM can search for relevant knowledge before answering.
     *
     * @param messages list of messages representing the conversation history
     * @param knowledgeTools the knowledge tools to register with the LLM
     * @return the generated AI response as a string
     */
    String generateResponse(List<Message> messages, KnowledgeTools knowledgeTools);

    /**
     * Generate an AI response with streaming capability.
     * Streams the response word by word as it's being generated.
     *
     * @param messages list of messages representing the conversation history
     * @param onChunk callback that receives each chunk of the response
     * @return the complete generated response
     */
    //String generateResponseStream(List<Message> messages, Consumer<String> onChunk);
}
