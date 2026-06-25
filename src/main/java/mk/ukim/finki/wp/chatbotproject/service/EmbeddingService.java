package mk.ukim.finki.wp.chatbotproject.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for generating text embeddings and computing vector similarity.
 * Uses Spring AI Ollama embedding model (nomic-embed-text).
 */
@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final ObjectMapper objectMapper;

    public EmbeddingService(EmbeddingModel embeddingModel, ObjectMapper objectMapper) {
        this.embeddingModel = embeddingModel;
        this.objectMapper = objectMapper;
    }

    /**
     * Generate an embedding vector for the given text.
     *
     * @param text input text
     * @return embedding as float array
     */
    public float[] embed(String text) {
        return embeddingModel.embed(text);
    }

    /**
     * Serialize an embedding vector to JSON for SQLite storage.
     *
     * @param embedding embedding vector
     * @return JSON string representation
     */
    public String serializeEmbedding(float[] embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize embedding", e);
        }
    }

    /**
     * Deserialize an embedding vector from JSON stored in SQLite.
     *
     * @param json JSON string representation
     * @return embedding as float array, or null if input is blank
     */
    public float[] deserializeEmbedding(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            List<Double> values = objectMapper.readValue(json, new TypeReference<>() {});
            float[] embedding = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                embedding[i] = values.get(i).floatValue();
            }
            return embedding;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize embedding", e);
        }
    }

    /**
     * Compute cosine similarity between two embedding vectors.
     *
     * @param a first vector
     * @param b second vector
     * @return cosine similarity in range [-1, 1], or 0 if vectors are incompatible
     */
    public double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0 || a.length != b.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
