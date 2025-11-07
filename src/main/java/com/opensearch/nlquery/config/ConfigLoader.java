package com.opensearch.nlquery.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration loader using Typesafe Config
 */
public class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    
    public static AppConfig load() {
        AppConfig appConfig = new AppConfig();
        
        try {
            // Load from application.conf, then override with environment variables
            Config config = ConfigFactory.load();
            
            // OpenSearch configuration
            appConfig.setOpensearchEndpoint(
                getEnvOrConfig("OPENSEARCH_ENDPOINT", config, "opensearch.endpoint", null)
            );
            appConfig.setRegion(
                getEnvOrConfig("AWS_REGION", config, "aws.region", "us-east-1")
            );
            appConfig.setDefaultIndex(
                getEnvOrConfig("OPENSEARCH_INDEX", config, "opensearch.defaultIndex", "documents")
            );
            appConfig.setMaxResults(
                getEnvOrConfigInt("MAX_RESULTS", config, "opensearch.maxResults", 10)
            );
            
            // Connection timeouts
            appConfig.setConnectionTimeout(
                getEnvOrConfigInt("CONNECTION_TIMEOUT", config, "opensearch.connectionTimeout", 5000)
            );
            appConfig.setSocketTimeout(
                getEnvOrConfigInt("SOCKET_TIMEOUT", config, "opensearch.socketTimeout", 10000)
            );
            
            // LLM configuration for NL to DSL conversion (using AWS Bedrock)
            appConfig.setUseLLMForConversion(
                getEnvOrConfigBoolean("USE_LLM_CONVERSION", config, "nl.conversion.useLLM", false)
            );
            appConfig.setBedrockModelId(
                getEnvOrConfig("BEDROCK_MODEL_ID", config, "nl.conversion.bedrock.modelId", "anthropic.claude-3-sonnet-20240229-v1:0")
            );
            appConfig.setBedrockRegion(
                getEnvOrConfig("BEDROCK_REGION", config, "nl.conversion.bedrock.region", appConfig.getRegion())
            );
            
            // Validate required fields
            if (appConfig.getOpensearchEndpoint() == null || appConfig.getOpensearchEndpoint().isEmpty()) {
                throw new IllegalArgumentException(
                    "OPENSEARCH_ENDPOINT must be set in environment variable or application.conf"
                );
            }
            
            logger.info("Configuration loaded - Endpoint: {}, Region: {}, Index: {}", 
                appConfig.getOpensearchEndpoint(), 
                appConfig.getRegion(), 
                appConfig.getDefaultIndex());
            
        } catch (Exception e) {
            logger.error("Failed to load configuration", e);
            throw new RuntimeException("Configuration loading failed", e);
        }
        
        return appConfig;
    }
    
    private static String getEnvOrConfig(String envVar, Config config, String configPath, String defaultValue) {
        String envValue = System.getenv(envVar);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }
        if (config.hasPath(configPath)) {
            return config.getString(configPath);
        }
        return defaultValue;
    }
    
    private static int getEnvOrConfigInt(String envVar, Config config, String configPath, int defaultValue) {
        String envValue = System.getenv(envVar);
        if (envValue != null && !envValue.isEmpty()) {
            try {
                return Integer.parseInt(envValue);
            } catch (NumberFormatException e) {
                logger.warn("Invalid integer value for {}: {}", envVar, envValue);
            }
        }
        if (config.hasPath(configPath)) {
            return config.getInt(configPath);
        }
        return defaultValue;
    }
    
    private static boolean getEnvOrConfigBoolean(String envVar, Config config, String configPath, boolean defaultValue) {
        String envValue = System.getenv(envVar);
        if (envValue != null && !envValue.isEmpty()) {
            return Boolean.parseBoolean(envValue);
        }
        if (config.hasPath(configPath)) {
            return config.getBoolean(configPath);
        }
        return defaultValue;
    }
}

