package mk.ukim.finki.wp.chatbotproject.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Message entity representing a single message in a chat conversation.
 */
@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    /**
     * Unique identifier for the message.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Role of the message sender (USER or AI).
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    /**
     * Content of the message.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Timestamp when the message was created.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    /**
     * Flag indicating if the message has been edited.
     * Only AI messages can be edited.
     */
    @Column(nullable = false)
    private Boolean edited = false;

    /**
     * Reference to the parent Chat.
     * Many-to-one relationship with Chat entity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    /**
     * Pre-persist callback to set timestamp.
     */
    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
        if (edited == null) {
            edited = false;
        }
    }
}

