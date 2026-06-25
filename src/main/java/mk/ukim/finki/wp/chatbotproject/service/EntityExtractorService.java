package mk.ukim.finki.wp.chatbotproject.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import mk.ukim.finki.wp.chatbotproject.models.ExtractedEntities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Dynamic entity extraction using Ollama LLM for semantic entities,
 * with regex only for basic types (years, numbers, currency codes/symbols).
 */
@Service
public class EntityExtractorService {

    private static final Logger log = LoggerFactory.getLogger(EntityExtractorService.class);

    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19|20)\\d{2}\\b");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b\\d+(?:\\.\\d+)?\\b");
    private static final Pattern CURRENCY_CODE_PATTERN = Pattern.compile("\\b[A-Z]{3}\\b");
    private static final Pattern CURRENCY_SYMBOL_PATTERN = Pattern.compile("[€$£¥₹₽]");

    private static final List<String> ENTITY_CATEGORIES = List.of(
            "products", "versions", "people", "locations", "technologies",
            "years", "numbers", "currencies"
    );

    private static final String SYSTEM_PROMPT = """
            You are a precise entity extraction system.
            Extract all meaningful entities from the user text and return ONLY valid JSON.
            Do not include markdown, comments, or explanation.
            Use empty arrays for categories with no matches.
            Normalize each value: lowercase, trimmed, single spaces between words.
            Preserve version numbers and product identifiers exactly (e.g. ".net 8", "iphone 16 pro max", "gpt-4").
            """;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public EntityExtractorService(ChatClient chatClient, ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Extract entities from text using LLM for semantic types and regex for basic types.
     *
     * @param text input text
     * @return map of entity category to normalized unique values
     */
    public Map<String, Set<String>> extractEntities(String text) {
        Map<String, Set<String>> entities = emptyEntityMap();

        if (text == null || text.isBlank()) {
            return entities;
        }

        Set<String> years = extractYears(text);
        entities.put("years", years);
        entities.put("numbers", extractNumbers(text, years));
        entities.put("currencies", extractCurrencies(text));

        ExtractedEntities llmEntities = extractViaLlm(text.trim());
        mergeInto(entities, "products", llmEntities.getProducts());
        mergeInto(entities, "versions", llmEntities.getVersions());
        mergeInto(entities, "people", llmEntities.getPeople());
        mergeInto(entities, "locations", llmEntities.getLocations());
        mergeInto(entities, "technologies", llmEntities.getTechnologies());
        mergeInto(entities, "numbers", llmEntities.getNumbers());
        mergeInto(entities, "currencies", llmEntities.getCurrencies());

        return entities;
    }

    /**
     * Entity validation hard filter.
     * If both sides have entities in the same category, they must match exactly.
     */
    public boolean areEntitiesCompatible(Map<String, Set<String>> queryEntities,
                                         Map<String, Set<String>> candidateEntities) {
        for (String category : ENTITY_CATEGORIES) {
            Set<String> querySet = normalizeSet(queryEntities.get(category));
            Set<String> candidateSet = normalizeSet(candidateEntities.get(category));

            if (querySet.isEmpty() || candidateSet.isEmpty()) {
                continue;
            }

            if (!querySet.equals(candidateSet)) {
                return false;
            }
        }
        return true;
    }

    public String serializeEntities(Map<String, Set<String>> entities) {
        try {
            return objectMapper.writeValueAsString(entities);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize entities", e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Set<String>> deserializeEntities(String json) {
        if (json == null || json.isBlank()) {
            return emptyEntityMap();
        }

        try {
            Map<String, List<String>> raw = objectMapper.readValue(json, Map.class);
            Map<String, Set<String>> entities = emptyEntityMap();

            for (String category : ENTITY_CATEGORIES) {
                List<String> values = raw.getOrDefault(category, List.of());
                mergeInto(entities, category, values);
            }

            // Backward compatibility with legacy "persons" key
            if (raw.containsKey("persons")) {
                mergeInto(entities, "people", raw.get("persons"));
            }

            return entities;
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize stored entities JSON", e);
            return emptyEntityMap();
        }
    }

    public List<String> entityCategories() {
        return ENTITY_CATEGORIES;
    }

    private ExtractedEntities extractViaLlm(String text) {
        String userPrompt = """
                Extract entities from this text into JSON with exactly these keys:
                products, versions, people, locations, technologies, numbers, currencies

                Category rules:
                - products: commercial products (e.g. iphone 16 pro max, galaxy s24)
                - versions: full version identifiers (e.g. .net 8, java 21, spring boot 3, gpt-4)
                - people: person names
                - locations: cities, countries, regions
                - technologies: technology/platform names (e.g. java, spring boot, gpt, ollama)
                - numbers: significant numeric values as strings (prices, quantities, counts)
                - currencies: currency codes or names (usd, eur, dollar)

                Text:
                %s
                """.formatted(text);

        try {
            String response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content();

            return parseLlmJson(response);
        } catch (Exception e) {
            log.error("LLM entity extraction failed for text: {}", text, e);
            throw new IllegalStateException("LLM entity extraction failed", e);
        }
    }

    private ExtractedEntities parseLlmJson(String response) throws JsonProcessingException {
        String json = extractJsonObject(response);
        return objectMapper.readValue(json, ExtractedEntities.class);
    }

    private String extractJsonObject(String response) {
        if (response == null || response.isBlank()) {
            throw new IllegalArgumentException("Empty LLM entity extraction response");
        }

        String trimmed = response.trim();

        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('{');
            int end = trimmed.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return trimmed.substring(start, end + 1);
            }
        }

        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }

        return trimmed;
    }

    private Set<String> extractYears(String text) {
        Set<String> years = new LinkedHashSet<>();
        Matcher matcher = YEAR_PATTERN.matcher(text);
        while (matcher.find()) {
            years.add(matcher.group());
        }
        return years;
    }

    private Set<String> extractNumbers(String text, Set<String> years) {
        Set<String> numbers = new LinkedHashSet<>();
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        while (matcher.find()) {
            String value = matcher.group();
            if (!years.contains(value)) {
                numbers.add(value);
            }
        }
        return numbers;
    }

    private Set<String> extractCurrencies(String text) {
        Set<String> currencies = new LinkedHashSet<>();

        Matcher codeMatcher = CURRENCY_CODE_PATTERN.matcher(text);
        while (codeMatcher.find()) {
            currencies.add(normalizeToken(codeMatcher.group()));
        }

        Matcher symbolMatcher = CURRENCY_SYMBOL_PATTERN.matcher(text);
        while (symbolMatcher.find()) {
            currencies.add(normalizeToken(symbolMatcher.group()));
        }

        return currencies;
    }

    private Map<String, Set<String>> emptyEntityMap() {
        Map<String, Set<String>> entities = new LinkedHashMap<>();
        for (String category : ENTITY_CATEGORIES) {
            entities.put(category, new LinkedHashSet<>());
        }
        return entities;
    }

    private void mergeInto(Map<String, Set<String>> target, String category, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        Set<String> set = target.computeIfAbsent(category, key -> new LinkedHashSet<>());
        for (String value : values) {
            String normalized = normalizeToken(value);
            if (!normalized.isBlank()) {
                set.add(normalized);
            }
        }
    }

    private Set<String> normalizeSet(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return values.stream()
                .map(this::normalizeToken)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizeToken(String value) {
        return value == null ? "" : value.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
