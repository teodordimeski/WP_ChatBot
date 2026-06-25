package mk.ukim.finki.wp.chatbotproject.service;

import mk.ukim.finki.wp.chatbotproject.models.SharedKnowledge;
import mk.ukim.finki.wp.chatbotproject.repository.SharedKnowledgeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Service for managing and searching shared knowledge using semantic RAG retrieval.
 * Applies LLM-driven entity validation as a hard filter before accepting cache hits.
 */
@Component
public class KnowledgeTools {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeTools.class);

    private static final double SIMILARITY_THRESHOLD = 0.80;
    private static final int TOP_CANDIDATES = 5;
    private static final int CACHE_TTL_DAYS = 2;

    private final SharedKnowledgeRepository sharedKnowledgeRepository;
    private final EmbeddingService embeddingService;
    private final EntityExtractorService entityExtractorService;

    public KnowledgeTools(SharedKnowledgeRepository sharedKnowledgeRepository,
                          EmbeddingService embeddingService,
                          EntityExtractorService entityExtractorService) {
        this.sharedKnowledgeRepository = sharedKnowledgeRepository;
        this.embeddingService = embeddingService;
        this.entityExtractorService = entityExtractorService;
    }

    /**
     * Search for knowledge using semantic RAG retrieval with entity validation.
     * Pipeline: embed query → linear scan recent entries → top 5 by cosine similarity
     * → filter by threshold, TTL, and entity compatibility → return first valid match.
     *
     * @param query the search query
     * @return the best matching answer, or "No relevant knowledge found." if no valid match
     */
    @Tool(description = "Search the shared knowledge base for a human-verified answer. "
            + "ALWAYS call this before answering any factual question. "
            + "If it returns a result, use that answer. "
            + "If it returns 'No relevant knowledge found.', answer from your own knowledge.")
    public String searchKnowledge(String query) {
        if (query == null || query.isBlank()) {
            return "No relevant knowledge found.";
        }

        Map<String, Set<String>> queryEntities;
        try {
            queryEntities = entityExtractorService.extractEntities(query.trim());
        } catch (IllegalStateException e) {
            log.warn("Entity extraction failed during knowledge search; rejecting cache lookup", e);
            return "No relevant knowledge found.";
        }

        float[] queryEmbedding = embeddingService.embed(query.trim());

        LocalDateTime cutoff = LocalDateTime.now().minusDays(CACHE_TTL_DAYS);
        List<SharedKnowledge> recentEntries = sharedKnowledgeRepository.findAllByCreatedAtAfter(cutoff);

        if (recentEntries.isEmpty()) {
            return "No relevant knowledge found.";
        }

        List<ScoredCandidate> candidates = new ArrayList<>();
        for (SharedKnowledge entry : recentEntries) {
            float[] entryEmbedding = embeddingService.deserializeEmbedding(entry.getEmbedding());
            if (entryEmbedding == null) {
                continue;
            }

            double similarity = embeddingService.cosineSimilarity(queryEmbedding, entryEmbedding);
            candidates.add(new ScoredCandidate(entry, similarity));
        }

        candidates.sort(Comparator.comparingDouble(ScoredCandidate::similarity).reversed());

        List<ScoredCandidate> topCandidates = candidates.stream()
                .limit(TOP_CANDIDATES)
                .toList();

        for (ScoredCandidate candidate : topCandidates) {
            SharedKnowledge entry = candidate.entry();

            if (candidate.similarity() < SIMILARITY_THRESHOLD) {
                continue;
            }

            if (entry.getCreatedAt().isBefore(cutoff)) {
                continue;
            }

            Map<String, Set<String>> entryEntities = entityExtractorService.deserializeEntities(entry.getEntities());
            if (!entityExtractorService.areEntitiesCompatible(queryEntities, entryEntities)) {
                continue;
            }

            return entry.getAnswer();
        }

        return "No relevant knowledge found.";
    }

    /**
     * Save or update knowledge in the repository.
     * Generates embedding and LLM-extracted entities on every save/update.
     *
     * @param question the question/query
     * @param answer   the answer to save
     */
    public void saveKnowledge(String question, String answer) {
        Optional<SharedKnowledge> existing = sharedKnowledgeRepository.findByQuestionExact(question);

        SharedKnowledge knowledge;
        if (existing.isPresent()) {
            knowledge = existing.get();
            knowledge.setAnswer(answer);
        } else {
            knowledge = new SharedKnowledge();
            knowledge.setQuestion(question);
            knowledge.setAnswer(answer);
        }

        float[] embedding = embeddingService.embed(question.trim());
        Map<String, Set<String>> entities = entityExtractorService.extractEntities(question.trim());

        knowledge.setEmbedding(embeddingService.serializeEmbedding(embedding));
        knowledge.setEntities(entityExtractorService.serializeEntities(entities));

        sharedKnowledgeRepository.save(knowledge);
    }

    private record ScoredCandidate(SharedKnowledge entry, double similarity) {
    }
}
