package com.opensearch.nlquery.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensearch.nlquery.config.AppConfig;
import com.opensearch.nlquery.model.SearchResult;
import org.apache.http.HttpHost;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import jakarta.json.stream.JsonParser;
import java.io.StringReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for interacting with AWS OpenSearch
 */
public class OpenSearchService {
    private static final Logger logger = LoggerFactory.getLogger(OpenSearchService.class);
    
    private final AppConfig config;
    private final ObjectMapper objectMapper;
    private final JacksonJsonpMapper jsonpMapper;
    private OpenSearchClient client;
    
    public OpenSearchService(AppConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.jsonpMapper = new JacksonJsonpMapper(objectMapper);
        initializeClient();
    }
    
    private void initializeClient() {
        try {
            URI endpoint = URI.create(config.getOpensearchEndpoint());
            int port = endpoint.getPort();
            if (port == -1) {
                // Default ports based on scheme
                port = "https".equals(endpoint.getScheme()) ? 443 : 80;
            }
            HttpHost httpHost = new HttpHost(endpoint.getHost(), port, endpoint.getScheme());
            
            // Build REST client
            RestClientBuilder builder = RestClient.builder(httpHost);
            
            // Set connection timeout
            builder.setRequestConfigCallback(requestConfigBuilder -> {
                requestConfigBuilder.setConnectTimeout(config.getConnectionTimeout());
                requestConfigBuilder.setSocketTimeout(config.getSocketTimeout());
                return requestConfigBuilder;
            });
            
            // For AWS OpenSearch, we would typically use AWS request signing
            // For now, using basic REST client - AWS signing can be added via custom interceptor
            RestClient restClient = builder.build();
            
            // Create OpenSearch transport with Jackson JSON mapper
            RestClientTransport transport = new RestClientTransport(restClient, jsonpMapper);
            
            // Create OpenSearch client
            this.client = new OpenSearchClient(transport);
            
            logger.info("OpenSearch client initialized for endpoint: {}", config.getOpensearchEndpoint());
            
        } catch (Exception e) {
            logger.error("Failed to initialize OpenSearch client: ", e);
            throw new RuntimeException("OpenSearch client initialization failed", e);
        }
    }
    
    /**
     * Execute search query against OpenSearch
     */
    public List<SearchResult> search(String dslQuery, String index) {
        logger.debug("Executing search on index: {} with query: {}", index, dslQuery);
        
        try {
            // Parse the DSL query JSON
            JsonNode queryJson = objectMapper.readTree(dslQuery);
            
            // Build search request
            SearchRequest.Builder requestBuilder = new SearchRequest.Builder()
                .index(index);
            
            // Extract query from DSL
            if (queryJson.has("query")) {
                JsonNode queryNode = queryJson.get("query");
                // Parse query JSON into Query object using JsonpMapper
                String queryJsonString = objectMapper.writeValueAsString(queryNode);
                try (JsonParser parser = jsonpMapper.jsonProvider().createParser(new StringReader(queryJsonString))) {
                    Query query = jsonpMapper.deserialize(parser, Query.class);
                    requestBuilder.query(query);
                }
            }
            
            // Extract size
            if (queryJson.has("size")) {
                requestBuilder.size(queryJson.get("size").asInt());
            }
            
            // Extract from
            if (queryJson.has("from")) {
                requestBuilder.from(queryJson.get("from").asInt());
            }
            
            SearchRequest request = requestBuilder.build();
            
            // Execute search
            @SuppressWarnings("rawtypes")
            SearchResponse<Map> response = client.search(request, Map.class);
            
            // Convert results
            List<SearchResult> results = new ArrayList<>();
            response.hits().hits().forEach(hit -> {
                SearchResult result = new SearchResult();
                result.setId(hit.id());
                result.setScore(hit.score());
                @SuppressWarnings("unchecked")
                Map<String, Object> source = (Map<String, Object>) hit.source();
                result.setSource(source);
                results.add(result);
            });
            
            logger.info("Search completed. Found {} results", results.size());
            return results;
            
        } catch (Exception e) {
            logger.error("Error executing search: ", e);
            throw new RuntimeException("Search execution failed", e);
        }
    }
    
    /**
     * Close the OpenSearch client
     */
    public void close() {
        try {
            if (client != null) {
                client._transport().close();
                client = null;
            }
        } catch (IOException e) {
            logger.error("Error closing OpenSearch client: ", e);
        }
    }
}

