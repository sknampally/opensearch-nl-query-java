package com.opensearch.nlquery.config;

/**
 * Application configuration model
 */
public class AppConfig {
    private String opensearchEndpoint;
    private String region;
    private String defaultIndex;
    private int maxResults;
    private boolean useLLMForConversion;
    private String bedrockModelId;
    private String bedrockRegion;
    private int connectionTimeout;
    private int socketTimeout;
    
    // Getters and Setters
    public String getOpensearchEndpoint() {
        return opensearchEndpoint;
    }
    
    public void setOpensearchEndpoint(String opensearchEndpoint) {
        this.opensearchEndpoint = opensearchEndpoint;
    }
    
    public String getRegion() {
        return region;
    }
    
    public void setRegion(String region) {
        this.region = region;
    }
    
    public String getDefaultIndex() {
        return defaultIndex;
    }
    
    public void setDefaultIndex(String defaultIndex) {
        this.defaultIndex = defaultIndex;
    }
    
    public int getMaxResults() {
        return maxResults;
    }
    
    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }
    
    public boolean isUseLLMForConversion() {
        return useLLMForConversion;
    }
    
    public void setUseLLMForConversion(boolean useLLMForConversion) {
        this.useLLMForConversion = useLLMForConversion;
    }
    
    public String getBedrockModelId() {
        return bedrockModelId;
    }
    
    public void setBedrockModelId(String bedrockModelId) {
        this.bedrockModelId = bedrockModelId;
    }
    
    public String getBedrockRegion() {
        return bedrockRegion;
    }
    
    public void setBedrockRegion(String bedrockRegion) {
        this.bedrockRegion = bedrockRegion;
    }
    
    public int getConnectionTimeout() {
        return connectionTimeout;
    }
    
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    
    public int getSocketTimeout() {
        return socketTimeout;
    }
    
    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }
}

