package com.opensearch.nlquery;

import com.opensearch.nlquery.config.ConfigLoader;
import com.opensearch.nlquery.config.AppConfig;
import com.opensearch.nlquery.service.NaturalLanguageQueryService;
import com.opensearch.nlquery.service.OpenSearchService;
import com.opensearch.nlquery.model.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Scanner;

/**
 * Main application entry point for OpenSearch Natural Language Query POC
 */
public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        logger.info("Starting OpenSearch Natural Language Query POC");
        
        try {
            // Load configuration
            AppConfig config = ConfigLoader.load();
            logger.info("Configuration loaded successfully");
            
            // Initialize services
            NaturalLanguageQueryService nlQueryService = new NaturalLanguageQueryService(config);
            OpenSearchService openSearchService = new OpenSearchService(config);
            
            // Interactive mode or command line mode
            if (args.length > 0) {
                // Command line mode: single query
                String query = String.join(" ", args);
                logger.info("Processing query: {}", query);
                processQuery(nlQueryService, openSearchService, query, config);
            } else {
                // Interactive mode
                runInteractiveMode(nlQueryService, openSearchService, config);
            }
            
        } catch (Exception e) {
            logger.error("Application error: ", e);
            System.exit(1);
        }
    }
    
    private static void runInteractiveMode(
            NaturalLanguageQueryService nlQueryService,
            OpenSearchService openSearchService,
            AppConfig config) {
        
        logger.info("=== OpenSearch Natural Language Query POC ===");
        logger.info("Enter natural language queries (type 'exit' to quit)");
        
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Query: ");
            String query = scanner.nextLine().trim();
            
            if (query.isEmpty()) {
                continue;
            }
            
            logger.info("User entered query: {}", query);
            
            if ("exit".equalsIgnoreCase(query) || "quit".equalsIgnoreCase(query)) {
                logger.info("User requested exit");
                break;
            }
            
            try {
                processQuery(nlQueryService, openSearchService, query, config);
            } catch (Exception e) {
                logger.error("Error processing query: {}", e.getMessage(), e);
            }
        }
        
        logger.info("Interactive mode ended");
        scanner.close();
    }
    
    private static void processQuery(
            NaturalLanguageQueryService nlQueryService,
            OpenSearchService openSearchService,
            String naturalLanguageQuery,
            AppConfig config) {
        
        try {
            // Step 1: Convert natural language to OpenSearch DSL
            logger.info("[1/3] Converting natural language to OpenSearch DSL...");
            logger.debug("Natural language query: {}", naturalLanguageQuery);
            String dslQuery = nlQueryService.convertToDSL(naturalLanguageQuery);
            logger.info("Generated DSL Query: {}", dslQuery);
            
            // Step 2: Execute query against OpenSearch
            logger.info("[2/3] Executing query against AWS OpenSearch...");
            logger.debug("Executing query on index: {}", config.getDefaultIndex());
            List<SearchResult> results = openSearchService.search(dslQuery, config.getDefaultIndex());
            
            // Step 3: Log results
            logger.info("[3/3] Search Results:");
            logger.info("Total hits: {}", results.size());
            
            if (results.isEmpty()) {
                logger.info("No results found.");
            } else {
                int maxResults = Math.min(results.size(), config.getMaxResults());
                logger.info("Displaying {} of {} results", maxResults, results.size());
                for (int i = 0; i < maxResults; i++) {
                    SearchResult result = results.get(i);
                    logger.info("--- Result {} ---", (i + 1));
                    logger.info("Score: {}", result.getScore());
                    logger.info("ID: {}", result.getId());
                    logger.info("Source: {}", result.getSource());
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in query processing pipeline: ", e);
            throw new RuntimeException("Failed to process query", e);
        }
    }
}

