package com.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportResult {
    private String summary;
    @JsonProperty("graph_items")
    private List<String> graphItems;
    private List<String> insights;
    private List<String> recommendations;

    public String getSummary() { return summary; }
    public List<String> getGraphItems() { return graphItems; }
    public List<String> getInsights() { return insights; }
    public List<String> getRecommendations() { return recommendations; }

    public static ReportResult fromJson(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(json, ReportResult.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse report JSON: " + json, e);
        }
    }
}
