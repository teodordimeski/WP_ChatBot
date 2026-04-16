package mk.ukim.finki.wp.chatbotproject.service.impl;

import mk.ukim.finki.wp.chatbotproject.models.Chat;
import mk.ukim.finki.wp.chatbotproject.models.Message;
import mk.ukim.finki.wp.chatbotproject.models.Role;
import mk.ukim.finki.wp.chatbotproject.repository.ChatRepository;
import mk.ukim.finki.wp.chatbotproject.repository.MessageRepository;
import mk.ukim.finki.wp.chatbotproject.service.MessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of MessageService.
 * Handles message creation, retrieval, and editing with proper business rules.
 */
@Service
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;

    public MessageServiceImpl(MessageRepository messageRepository, ChatRepository chatRepository) {
        this.messageRepository = messageRepository;
        this.chatRepository = chatRepository;
    }

    @Override
    public Message saveMessage(Chat chat, Role role, String content) {
        Message message = new Message();
        message.setChat(chat);
        message.setRole(role);
        message.setContent(content);

        return messageRepository.save(message);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getMessagesByChat(Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found with ID: " + chatId));

        return messageRepository.findByChatOrderByTimestampAsc(chat);
    }

    @Override
    public Message editMessage(Long messageId, String newContent) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found with ID: " + messageId));

        // USER messages cannot be edited
        if (message.getRole() == Role.USER) {
            throw new IllegalArgumentException("USER messages cannot be edited after saving");
        }

        // Only AI messages can be edited
        message.setContent(newContent);

        return messageRepository.save(message);
    }

    @Override
    @Transactional(readOnly = true)
    public Message getMessageById(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found with ID: " + messageId));
    }
}

