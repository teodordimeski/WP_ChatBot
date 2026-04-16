package mk.ukim.finki.wp.chatbotproject.service;

import mk.ukim.finki.wp.chatbotproject.models.Chat;

/**
 * Service interface for Chat operations.
 */
public interface ChatService {

    /**
     * Create a new chat with the given title.
     *
     * @param title the title of the chat
     * @return the created Chat entity
     */
    Chat createChat(String title);

    /**
     * Retrieve a chat by ID.
     *
     * @param id the ID of the chat
     * @return the Chat entity
     * @throws jakarta.persistence.EntityNotFoundException if not found
     */
    Chat getChatById(Long id);

    /**
     * Send a message in a chat.
     * This method:
     * 1. Saves the USER message to the database
     * 2. Generates an AI response using the full conversation history
     * 3. Saves the AI response to the database
     *
     * @param chatId the ID of the chat
     * @param userInput the user message content
     * @return the Chat entity with updated messages
     * @throws IllegalArgumentException if chat not found
     */
    Chat sendMessage(Long chatId, String userInput);
}

