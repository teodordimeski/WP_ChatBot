package mk.ukim.finki.wp.chatbotproject.service.impl;

import mk.ukim.finki.wp.chatbotproject.models.Chat;
import mk.ukim.finki.wp.chatbotproject.models.Message;
import mk.ukim.finki.wp.chatbotproject.models.Role;
import mk.ukim.finki.wp.chatbotproject.repository.ChatRepository;
import mk.ukim.finki.wp.chatbotproject.service.ChatService;
import mk.ukim.finki.wp.chatbotproject.service.LLMService;
import mk.ukim.finki.wp.chatbotproject.service.MessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of ChatService.
 * Orchestrates the conversation flow between user and AI.
 */
@Service
@Transactional
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final MessageService messageService;
    private final LLMService llmService;

    public ChatServiceImpl(ChatRepository chatRepository, MessageService messageService, LLMService llmService) {
        this.chatRepository = chatRepository;
        this.messageService = messageService;
        this.llmService = llmService;
    }

    @Override
    public Chat createChat(String title) {
        Chat chat = new Chat();
        chat.setTitle(title);

        return chatRepository.save(chat);
    }

    @Override
    @Transactional(readOnly = true)
    public Chat getChatById(Long id) {
        return chatRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found with ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Chat> getAllChats() {
        return chatRepository.findAll();
    }

    @Override
    public Chat sendMessage(Long chatId, String userInput) {
        // Retrieve the chat
        Chat chat = getChatById(chatId);

        // Step 1: Save the USER message
        messageService.saveMessage(chat, Role.USER, userInput);

        // Step 2: Retrieve all messages for context (including the newly saved user message)
        List<Message> conversationHistory = messageService.getMessagesByChat(chatId);

        // Step 3: Generate AI response using full conversation history
        String aiResponse = llmService.generateResponse(conversationHistory);

        // Step 4: Save the AI response
        messageService.saveMessage(chat, Role.AI, aiResponse);

        // Return the updated chat
        return getChatById(chatId);
    }

    @Override
    public void deleteChat(Long chatId) {
        Chat chat = getChatById(chatId);
        chatRepository.delete(chat);
    }
}

