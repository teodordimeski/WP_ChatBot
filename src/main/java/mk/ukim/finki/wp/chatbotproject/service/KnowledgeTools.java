package mk.ukim.finki.wp.chatbotproject.service;

import mk.ukim.finki.wp.chatbotproject.models.SharedKnowledge;
import mk.ukim.finki.wp.chatbotproject.repository.SharedKnowledgeRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing and searching shared knowledge.
 * Provides tool methods for AI integration and knowledge persistence.
 */
@Component
public class KnowledgeTools {

    private final SharedKnowledgeRepository sharedKnowledgeRepository;

    public KnowledgeTools(SharedKnowledgeRepository sharedKnowledgeRepository) {
        this.sharedKnowledgeRepository = sharedKnowledgeRepository;
    }

    /**
     * Search for knowledge in the repository based on a query.
     * This method can be called by the LLM as a tool.
     *
     * @param query the search query
     * @return the best matching answer, or "No relevant knowledge found." if no results
     */
    @Tool(description = "Search the shared knowledge base for a human-verified answer. "
            + "ALWAYS call this before answering any factual question. "
            + "If it returns a result, use that answer. "
            + "If it returns 'No relevant knowledge found.', answer from your own knowledge.")
    public String searchKnowledge(String query) {
        List<SharedKnowledge> results = sharedKnowledgeRepository.searchByQuestion(query);
        
        if (results.isEmpty()) {
            return "No relevant knowledge found.";
        }
        
        // Return the answer from the most relevant (first) result
        return results.get(0).getAnswer();
    }

    /**
     * Save or update knowledge in the repository.
     * If an exact match for the question exists, updates its answer.
     * Otherwise, creates a new knowledge entry.
     *
     * @param question the question/query
     * @param answer the answer to save
     */
    public void saveKnowledge(String question, String answer) {
        Optional<SharedKnowledge> existing = sharedKnowledgeRepository.findByQuestionExact(question);
        
        if (existing.isPresent()) {
            // Update existing entry
            SharedKnowledge knowledge = existing.get();
            knowledge.setAnswer(answer);
            sharedKnowledgeRepository.save(knowledge);
        } else {
            // Create new entry
            SharedKnowledge knowledge = new SharedKnowledge();
            knowledge.setQuestion(question);
            knowledge.setAnswer(answer);
            sharedKnowledgeRepository.save(knowledge);
        }
    }
}
