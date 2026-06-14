package mk.ukim.finki.wp.chatbotproject.repository;

import mk.ukim.finki.wp.chatbotproject.models.Chat;
import mk.ukim.finki.wp.chatbotproject.models.Message;
import mk.ukim.finki.wp.chatbotproject.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Message entity.
 * Provides CRUD operations and custom query methods for Message.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Fetch all messages for a given chat ordered by timestamp in ascending order.
     *
     * @param chat the Chat entity to fetch messages for
     * @return list of messages ordered by timestamp ascending
     */
    List<Message> findByChatOrderByTimestampAsc(Chat chat);

    /**
     * Find the last AI message for a given chat (most recent by timestamp).
     *
     * @param chatId the ID of the chat
     * @return the most recent AI message for the chat
     */
    @Query("SELECT m FROM Message m WHERE m.chat.id = :chatId AND m.role = :role ORDER BY m.timestamp DESC LIMIT 1")
    Optional<Message> findLastMessageByChat(@Param("chatId") Long chatId, @Param("role") Role role);

    /**
     * Find the most recent USER message in a chat that came before the given timestamp.
     *
     * @param chat the Chat entity to search in
     * @param timestamp the timestamp to search before
     * @return the most recent USER message before the given timestamp
     */
    @Query("SELECT m FROM Message m WHERE m.chat = :chat AND m.role = :role AND m.timestamp < :timestamp ORDER BY m.timestamp DESC LIMIT 1")
    Optional<Message> findMostRecentUserMessageBefore(@Param("chat") Chat chat, @Param("role") Role role, @Param("timestamp") LocalDateTime timestamp);
}

