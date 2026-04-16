package mk.ukim.finki.wp.chatbotproject.service;

import mk.ukim.finki.wp.chatbotproject.models.Chat;
import mk.ukim.finki.wp.chatbotproject.models.Message;
import mk.ukim.finki.wp.chatbotproject.models.Role;

import java.util.List;

/**
 * Service interface for Message operations.
 */
public interface MessageService {

    /**
     * Save a message to the database.
     *
     * @param chat the Chat entity the message belongs to
     * @param role the Role of the message sender (USER or AI)
     * @param content the content of the message
     * @return the saved Message entity
     */
    Message saveMessage(Chat chat, Role role, String content);

    /**
     * Retrieve all messages for a given chat ordered by timestamp.
     *
     * @param chatId the ID of the chat
     * @return list of messages ordered by timestamp ascending
     */
    List<Message> getMessagesByChat(Long chatId);

    /**
     * Edit a message content.
     * Only AI messages can be edited. USER messages cannot be edited.
     *
     * @param messageId the ID of the message to edit
     * @param newContent the new content for the message
     * @return the updated Message entity
     * @throws IllegalArgumentException if the message is a USER message
     * @throws jakarta.persistence.EntityNotFoundException if message not found
     */
    Message editMessage(Long messageId, String newContent);

    /**
     * Retrieve a message by ID.
     *
     * @param messageId the ID of the message
     * @return the Message entity
     * @throws jakarta.persistence.EntityNotFoundException if not found
     */
    Message getMessageById(Long messageId);
}

