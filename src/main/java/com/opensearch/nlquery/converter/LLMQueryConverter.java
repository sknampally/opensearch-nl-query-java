package com.opensearch.nlquery.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensearch.nlquery.config.AppConfig;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * LLM-based converter using AWS Bedrock to convert natural language to OpenSearch DSL
 * Uses direct HTTP calls with AWS request signing (no bedrock-runtime dependency required)
 */
public class LLMQueryConverter {
    private static final Logger logger = LoggerFactory.getLogger(LLMQueryConverter.class);
    
    private final AppConfig config;
    private final ObjectMapper objectMapper;
    private final Region bedrockRegion;
    private final Aws4Signer signer;
    private final DefaultCredentialsProvider credentialsProvider;
    
    private static final String SYSTEM_PROMPT = 
        "You are an expert at converting natural language queries into OpenSearch DSL (Domain Specific Language) queries.\n" +
        "\n" +
        "Your task is to convert user queries into valid OpenSearch query JSON format.\n" +
        "\n" +
        "Rules:\n" +
        "1. Return ONLY valid JSON in OpenSearch DSL format\n" +
        "2. Use appropriate query types (match, match_phrase, term, range, bool, etc.)\n" +
        "3. For text searches, prefer 'match' or 'multi_match' queries\n" +
        "4. For exact matches, use 'term' or 'terms' queries\n" +
        "5. For date/number ranges, use 'range' queries\n" +
        "6. Combine multiple conditions using 'bool' query with 'must', 'should', 'must_not', 'filter'\n" +
        "7. Always include a 'size' parameter (default: 10)\n" +
        "8. Do not include any explanations or markdown formatting, only the JSON\n" +
        "\n" +
        "Example output format:\n" +
        "{\n" +
        "  \"query\": {\n" +
        "    \"bool\": {\n" +
        "      \"must\": [\n" +
        "        {\n" +
        "          \"match\": {\n" +
        "            \"_all\": {\n" +
        "              \"query\": \"search terms\",\n" +
        "              \"operator\": \"and\"\n" +
        "            }\n" +
        "          }\n" +
        "        }\n" +
        "      ]\n" +
        "    }\n" +
        "  },\n" +
        "  \"size\": 10\n" +
        "}\n";
    
    public LLMQueryConverter(AppConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.bedrockRegion = Region.of(config.getBedrockRegion() != null ? config.getBedrockRegion() : config.getRegion());
        this.credentialsProvider = DefaultCredentialsProvider.create();
        this.signer = Aws4Signer.create();
        
        logger.info("Bedrock client initialized with model: {} in region: {}", 
            config.getBedrockModelId(), bedrockRegion);
    }
    
    /**
     * Convert natural language query to OpenSearch DSL using AWS Bedrock
     */
    public String convert(String naturalLanguageQuery) {
        logger.debug("Converting query using Bedrock: {}", naturalLanguageQuery);
        
        try {
            String userPrompt = String.format(
                "Convert the following natural language query to OpenSearch DSL:\n\n%s",
                naturalLanguageQuery
            );
            
            // Build request payload for Claude (Anthropic) models
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("anthropic_version", "bedrock-2023-05-31");
            
            // Build messages array - Claude uses system and user messages
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", SYSTEM_PROMPT);
            
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);
            
            requestBody.put("messages", new Object[]{systemMessage, userMessage});
            requestBody.put("max_tokens", 1000);
            requestBody.put("temperature", 0.1);
            
            String requestBodyJson = objectMapper.writeValueAsString(requestBody);
            
            // Build Bedrock endpoint URL
            String endpoint = String.format("https://bedrock-runtime.%s.amazonaws.com/model/%s/invoke", 
                bedrockRegion.id(), config.getBedrockModelId());
            
            // Create HTTP request
            SdkHttpFullRequest.Builder requestBuilder = SdkHttpFullRequest.builder()
                .uri(URI.create(endpoint))
                .method(SdkHttpMethod.POST)
                .putHeader("Content-Type", "application/json")
                .putHeader("Accept", "application/json")
                .putHeader("X-Amz-Date", Instant.now().toString())
                .contentStreamProvider(() -> 
                    new java.io.ByteArrayInputStream(requestBodyJson.getBytes(StandardCharsets.UTF_8)));
            
            SdkHttpFullRequest request = requestBuilder.build();
            
            // Sign the request
            Aws4SignerParams signerParams = Aws4SignerParams.builder()
                .signingName("bedrock")
                .signingRegion(bedrockRegion)
                .awsCredentials(credentialsProvider.resolveCredentials())
                .build();
            
            SdkHttpFullRequest signedRequest = signer.sign(request, signerParams);
            
            // Execute signed request using Apache HttpClient
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost(signedRequest.getUri());
                
                // Copy headers from signed request
                signedRequest.headers().forEach((key, values) -> {
                    values.forEach(value -> httpPost.addHeader(key, value));
                });
                
                httpPost.setEntity(new StringEntity(requestBodyJson, org.apache.hc.core5.http.ContentType.APPLICATION_JSON));
                
                // Send request
                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    int statusCode = response.getCode();
                    String responseBody = EntityUtils.toString(response.getEntity());
                    
                    if (statusCode != 200) {
                        throw new RuntimeException("Bedrock API error: " + statusCode + " - " + responseBody);
                    }
                    
                    // Parse response
                    @SuppressWarnings("unchecked")
                    Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                    
                    // Extract content from Claude response
                    @SuppressWarnings("unchecked")
                    java.util.List<Map<String, Object>> contentList = (java.util.List<Map<String, Object>>) responseMap.get("content");
                    if (contentList == null || contentList.isEmpty()) {
                        throw new RuntimeException("Empty response from Bedrock");
                    }
                    
                    Map<String, Object> contentBlock = contentList.get(0);
                    String content = (String) contentBlock.get("text");
                    
                    // Clean the response (remove markdown code blocks if present)
                    content = content.trim();
                    if (content.startsWith("```json")) {
                        content = content.substring(7);
                    }
                    if (content.startsWith("```")) {
                        content = content.substring(3);
                    }
                    if (content.endsWith("```")) {
                        content = content.substring(0, content.length() - 3);
                    }
                    content = content.trim();
                    
                    // Validate JSON
                    objectMapper.readTree(content);
                    
                    logger.debug("Bedrock generated DSL: {}", content);
                    return content;
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in Bedrock conversion: ", e);
            throw new RuntimeException("Bedrock query conversion failed", e);
        }
    }
}
