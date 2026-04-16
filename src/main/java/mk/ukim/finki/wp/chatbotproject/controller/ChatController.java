package mk.ukim.finki.wp.chatbotproject.controller;

import mk.ukim.finki.wp.chatbotproject.models.Chat;
import mk.ukim.finki.wp.chatbotproject.models.Message;
import mk.ukim.finki.wp.chatbotproject.service.ChatService;
import mk.ukim.finki.wp.chatbotproject.service.MessageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for chat-related HTTP requests.
 * Handles chat creation, viewing, message sending, and message editing.
 * All business logic is delegated to ChatService and MessageService.
 */
@Controller
@RequestMapping
public class ChatController {

    private final ChatService chatService;
    private final MessageService messageService;

    public ChatController(ChatService chatService, MessageService messageService) {
        this.chatService = chatService;
        this.messageService = messageService;
    }

    /**
     * List all chats.
     * GET /
     *
     * @param model the model to pass data to the view
     * @return the index view name
     */
    @GetMapping("/")
    public String listAllChats(Model model) {
        List<Chat> chats = chatService.getAllChats();
        model.addAttribute("chats", chats);
        return "index";
    }

    /**
     * Create a new chat.
     * POST /chat/create
     * Form parameter: title
     *
     * @param title the title of the new chat
     * @return redirect to home page
     */
    @PostMapping("/chat/create")
    public String createChat(@RequestParam String title) {
        chatService.createChat(title);
        return "redirect:/";
    }

    /**
     * Open a specific chat and display its messages.
     * GET /chat/{chatId}
     *
     * @param chatId the ID of the chat to open
     * @param model the model to pass data to the view
     * @return the chat view name
     */
    @GetMapping("/chat/{chatId}")
    public String openChat(@PathVariable Long chatId, Model model) {
        Chat chat = chatService.getChatById(chatId);
        List<Message> messages = messageService.getMessagesByChat(chatId);

        model.addAttribute("chat", chat);
        model.addAttribute("messages", messages);

        return "chat";
    }

    /**
     * Send a user message and trigger AI response generation.
     * POST /chat/{chatId}/send
     * Form parameter: content (the message content)
     *
     * Internally delegates to ChatService.sendMessage() which:
     * 1. Saves the USER message
     * 2. Calls LLMService to generate AI response with full context
     * 3. Saves the AI response
     *
     * @param chatId the ID of the chat
     * @param content the message content from the user
     * @return redirect to the chat page
     */
    @PostMapping("/chat/{chatId}/send")
    public String sendMessage(@PathVariable Long chatId, @RequestParam String content) {
        chatService.sendMessage(chatId, content);
        return "redirect:/chat/" + chatId;
    }

    /**
     * Edit an AI message content.
     * POST /message/{messageId}/edit
     * Form parameter: newContent (the updated message content)
     *
     * The MessageService enforces the rule that only AI messages can be edited.
     * USER messages will throw an exception if attempted to be edited.
     *
     * @param messageId the ID of the message to edit
     * @param newContent the new content for the message
     * @return redirect to the chat page containing this message
     */
    @PostMapping("/message/{messageId}/edit")
    public String editMessage(@PathVariable Long messageId, @RequestParam String newContent) {
        Message message = messageService.getMessageById(messageId);
        Long chatId = message.getChat().getId();

        messageService.editMessage(messageId, newContent);

        return "redirect:/chat/" + chatId;
    }

    /**
     * Delete a chat.
     * POST /chat/{chatId}/delete
     *
     * @param chatId the ID of the chat to delete
     * @return redirect to home page
     */
    @PostMapping("/chat/{chatId}/delete")
    public String deleteChat(@PathVariable Long chatId) {
        chatService.deleteChat(chatId);
        return "redirect:/";
    }

    /**
     * Exception handler for IllegalArgumentException.
     * Handles cases where chat or message is not found, or business rules are violated.
     *
     * @param ex the exception
     * @param model the model to pass error information to the view
     * @return the error view name
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        return "error";
    }
}

