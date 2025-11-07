package com.opensearch.nlquery.model;

import java.util.Map;

/**
 * Model representing a search result from OpenSearch
 */
public class SearchResult {
    private String id;
    private Double score;
    private Map<String, Object> source;
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public Double getScore() {
        return score;
    }
    
    public void setScore(Double score) {
        this.score = score;
    }
    
    public Map<String, Object> getSource() {
        return source;
    }
    
    public void setSource(Map<String, Object> source) {
        this.source = source;
    }
    
    @Override
    public String toString() {
        return "SearchResult{" +
            "id='" + id + '\'' +
            ", score=" + score +
            ", source=" + source +
            '}';
    }
}

