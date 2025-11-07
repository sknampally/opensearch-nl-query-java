package com.opensearch.nlquery.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Rule-based converter for natural language to OpenSearch DSL
 * Handles common query patterns without requiring LLM
 */
public class RuleBasedQueryConverter {
    private static final Logger logger = LoggerFactory.getLogger(RuleBasedQueryConverter.class);
    private final ObjectMapper objectMapper;
    
    // Common patterns
    private static final Pattern FILTER_PATTERN = Pattern.compile(
        "(where|with|that|having) (.+)", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern RANGE_PATTERN = Pattern.compile(
        "(between|from|after|before|greater than|less than|>|<|>=|<=) (.+)", Pattern.CASE_INSENSITIVE
    );
    
    public RuleBasedQueryConverter() {
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Convert natural language query to OpenSearch DSL
     */
    public String convert(String naturalLanguageQuery) {
        logger.debug("Converting query: {}", naturalLanguageQuery);
        
        try {
            ObjectNode query = objectMapper.createObjectNode();
            
            // Clean the query
            String cleanedQuery = naturalLanguageQuery.trim();
            
            // Extract main search terms
            String searchTerms = extractSearchTerms(cleanedQuery);
            
            // Build match query (default)
            ObjectNode matchQuery = objectMapper.createObjectNode();
            matchQuery.put("query", searchTerms);
            matchQuery.put("operator", "and");
            
            ObjectNode match = objectMapper.createObjectNode();
            match.set("_all", matchQuery);
            
            ObjectNode boolQuery = objectMapper.createObjectNode();
            boolQuery.set("must", objectMapper.createArrayNode().add(match));
            
            // Check for filters
            if (hasFilters(cleanedQuery)) {
                ObjectNode filter = buildFilters(cleanedQuery);
                if (filter != null && filter.size() > 0) {
                    boolQuery.set("filter", objectMapper.createArrayNode().add(filter));
                }
            }
            
            query.set("query", objectMapper.createObjectNode().set("bool", boolQuery));
            
            // Add default size
            query.put("size", 10);
            
            String result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(query);
            logger.debug("Generated DSL: {}", result);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error in rule-based conversion: ", e);
            // Fallback to simple match_all with query_string
            return createFallbackQuery(naturalLanguageQuery);
        }
    }
    
    private String extractSearchTerms(String query) {
        // Remove common query words
        String cleaned = query
            .replaceAll("^(find|search for|get|show me|list|find all|search) ", "")
            .replaceAll(" (where|with|that|having) .+$", "")
            .trim();
        
        return cleaned.isEmpty() ? "*" : cleaned;
    }
    
    private boolean hasFilters(String query) {
        return FILTER_PATTERN.matcher(query).find() || 
               RANGE_PATTERN.matcher(query).find();
    }
    
    private ObjectNode buildFilters(String query) {
        // Simplified implementation - returns empty filter
        // Can be extended with more sophisticated parsing in the future
        return objectMapper.createObjectNode();
    }
    
    private String createFallbackQuery(String naturalLanguageQuery) {
        try {
            ObjectNode query = objectMapper.createObjectNode();
            
            ObjectNode queryString = objectMapper.createObjectNode();
            queryString.put("query", naturalLanguageQuery);
            queryString.put("default_field", "_all");
            
            query.set("query", objectMapper.createObjectNode().set("query_string", queryString));
            query.put("size", 10);
            
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(query);
        } catch (Exception e) {
            logger.error("Error creating fallback query: ", e);
            throw new RuntimeException("Failed to create query", e);
        }
    }
}

