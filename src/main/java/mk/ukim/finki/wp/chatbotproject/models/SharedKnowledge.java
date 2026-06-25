package mk.ukim.finki.wp.chatbotproject.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SharedKnowledge entity representing a human-corrected Q&A pair.
 * These are answers that have been verified and corrected by users,
 * shared globally across all users.
 */
@Entity
@Table(name = "shared_knowledge")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedKnowledge {

    /**
     * Unique identifier for the shared knowledge entry.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Question/query that was corrected.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    /**
     * Human-corrected answer.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    /**
     * Embedding vector serialized as JSON for semantic search.
     */
    @Column(columnDefinition = "TEXT")
    private String embedding;

    /**
     * Extracted entities serialized as JSON for entity-aware cache validation.
     */
    @Column(columnDefinition = "TEXT")
    private String entities;

    /**
     * Timestamp when this knowledge entry was created.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Pre-persist callback to set creation timestamp.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
