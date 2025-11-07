package com.opensearch.nlquery.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opensearch.nlquery.config.AppConfig;
import com.opensearch.nlquery.converter.LLMQueryConverter;
import com.opensearch.nlquery.converter.RuleBasedQueryConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for converting natural language queries to OpenSearch DSL
 */
public class NaturalLanguageQueryService {
    private static final Logger logger = LoggerFactory.getLogger(NaturalLanguageQueryService.class);
    
    private final LLMQueryConverter llmConverter;
    private final RuleBasedQueryConverter ruleBasedConverter;
    private final ObjectMapper objectMapper;
    
    public NaturalLanguageQueryService(AppConfig config) {
        this.objectMapper = new ObjectMapper();
        
        // Initialize converters
        if (config.isUseLLMForConversion() && config.getBedrockModelId() != null) {
            this.llmConverter = new LLMQueryConverter(config);
            this.ruleBasedConverter = null;
            logger.info("Using LLM-based query conversion with Bedrock model: {}", config.getBedrockModelId());
        } else {
            this.llmConverter = null;
            this.ruleBasedConverter = new RuleBasedQueryConverter();
            logger.info("Using rule-based query conversion");
        }
    }
    
    /**
     * Convert natural language query to OpenSearch DSL JSON
     */
    public String convertToDSL(String naturalLanguageQuery) {
        try {
            String dslQuery;
            
            if (llmConverter != null) {
                // Use LLM for conversion
                dslQuery = llmConverter.convert(naturalLanguageQuery);
            } else {
                // Use rule-based conversion
                dslQuery = ruleBasedConverter.convert(naturalLanguageQuery);
            }
            
            // Validate and format JSON
            ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(dslQuery);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
            
        } catch (Exception e) {
            logger.error("Failed to convert natural language to DSL: ", e);
            throw new RuntimeException("Query conversion failed", e);
        }
    }
}

