package mk.ukim.finki.wp.chatbotproject.repository;

import mk.ukim.finki.wp.chatbotproject.models.SharedKnowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SharedKnowledge entity.
 * Provides CRUD operations and custom query methods for shared knowledge entries.
 */
@Repository
public interface SharedKnowledgeRepository extends JpaRepository<SharedKnowledge, Long> {

    /**
     * Search for knowledge entries by question using case-insensitive LIKE.
     * Returns up to 3 most relevant results ordered by creation date (newest first).
     *
     * @param query the search query
     * @return list of up to 3 matching knowledge entries
     */
    @Query("SELECT sk FROM SharedKnowledge sk WHERE LOWER(sk.question) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY sk.createdAt DESC LIMIT 3")
    List<SharedKnowledge> searchByQuestion(@Param("query") String query);

    /**
     * Find a knowledge entry by exact question match (case-insensitive).
     *
     * @param question the exact question to search for
     * @return Optional containing the knowledge entry if found
     */
    @Query("SELECT sk FROM SharedKnowledge sk WHERE LOWER(sk.question) = LOWER(:question)")
    Optional<SharedKnowledge> findByQuestionExact(@Param("question") String question);
}
