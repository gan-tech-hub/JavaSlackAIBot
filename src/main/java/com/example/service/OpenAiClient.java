package com.example.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenAiClient {
    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public OpenAiClient(@Value("${openai.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
    }

    public ReportResult generateReport(String reportType, String period, String csvData) {
        try {
            String systemPrompt = """
                    You are an AI assistant that generates business reports based on structured data.
                    
                    Your tasks:
                    1. Identify key metrics from the provided data.
                    2. Suggest which metrics should be visualized in a graph.
                    3. Generate a clear and concise report in Japanese.
                    4. Provide insights and recommendations.
                    
                    Output must be in JSON format:
                    {
                      "summary": "日本語の要約",
                      "graph_items": ["..."],
                      "insights": ["..."],
                      "recommendations": ["..."]
                    }
                    """;

            String userPrompt = """
                    Report type: %s
                    Target period: %s
                    
                    Here is the dataset:
                    %s
                    """.formatted(reportType, period, csvData);

            log.info("[Report] callOpenAi Call Start");
            // ★ ChatCompletion 呼び出し
            String response = callOpenAi(systemPrompt, userPrompt);

            // コードブロック除去
            String cleaned = response
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            // ★ JSON をパースして ReportResult に変換
            return ReportResult.fromJson(cleaned);
        } catch (Exception e) {
            log.info("[Report] ReportResult Exception Catch");
            throw new RuntimeException("ReportResult Call Failed", e);
        }
    }

    public List<double[]> embed(List<String> texts) {
        try {
            // JSONリクエスト構築
            ObjectNode root = mapper.createObjectNode();
            root.put("model", "text-embedding-3-small");
            // inputにList<String>をそのまま渡す
            root.set("input", mapper.valueToTree(texts));

            String body = mapper.writeValueAsString(root);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/embeddings"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(response.body());

            if (json.has("error")) {
                throw new RuntimeException("OpenAI APIエラー: " + json.get("error").get("message").asText());
            }

            // data配列に各テキストのembeddingが入っている
            List<double[]> embeddings = new ArrayList<>();
            for (JsonNode dataNode : json.get("data")) {
                JsonNode embeddingNode = dataNode.get("embedding");
                double[] embedding = new double[embeddingNode.size()];
                for (int i = 0; i < embeddingNode.size(); i++) {
                    embedding[i] = embeddingNode.get(i).asDouble();
                }
                embeddings.add(embedding);
            }

            return embeddings;

        } catch (Exception e) {
            throw new RuntimeException("Embeddings API呼び出し失敗", e);
        }
    }

    public String chat(String context, String question) {
        try {
            String prompt = "以下のFAQを参考に質問に答えてください:\n" + context + "\n質問: " + question;

            ObjectNode root = mapper.createObjectNode();
            root.put("model", "ft:gpt-3.5-turbo-0125:testplatform::CoBT8aLN");

            ArrayNode messages = mapper.createArrayNode();

            ObjectNode systemMsg = mapper.createObjectNode();
            systemMsg.put("role", "system");
            systemMsg.put("content", "あなたはFAQに基づいて回答するアシスタントです。");
            messages.add(systemMsg);

            ObjectNode userMsg = mapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);
            messages.add(userMsg);

            root.set("messages", messages);

            String body = mapper.writeValueAsString(root);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(response.body());

            if (json.has("error")) {
                return "OpenAI APIエラー: " + json.get("error").get("message").asText();
            }

            return json.get("choices").get(0).get("message").get("content").asText();

        } catch (Exception e) {
            throw new RuntimeException("OpenAI API呼び出し失敗", e);
        }
    }

    public String summarize(List<String> representatives) {
        if (representatives == null || representatives.isEmpty()) {
            return "要約対象のメッセージがありません。";
        }

        // 代表メッセージを連結してプロンプトを作成
        String joined = representatives.stream()
                .map(msg -> "- " + msg)
                .collect(Collectors.joining("\n"));

        String userPrompt = "Summarize: Slack thread about summary";

        // OpenAIのChatCompletion APIを呼び出す（既存のメソッドを利用）
        return callChatCompletion(userPrompt, joined);
    }

    private String callOpenAi(String systemPrompt, String userPrompt) {
        log.info("[Report] callOpenAi START");
        try {
            ObjectNode root = mapper.createObjectNode();
            root.put("model", "gpt-4o");

            ArrayNode messages = mapper.createArrayNode();

            ObjectNode systemMsg = mapper.createObjectNode();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);

            ObjectNode userMsg = mapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.put("content", userPrompt);
            messages.add(userMsg);

            root.set("messages", messages);

            String body = mapper.writeValueAsString(root);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            log.info("[OpenAI RAW] {}", response.body());

            JsonNode json = mapper.readTree(response.body());

            // content を取得
            String content = json.get("choices").get(0).get("message").get("content").asText();

            // コードブロック除去
            String cleaned = content
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            return cleaned;

        } catch (Exception e) {
            log.info("[Report] Exception Catch: OpenAI API Call Failed");
            throw new RuntimeException("OpenAI API呼び出し失敗", e);
        }
    }

    private String callChatCompletion(String userPrompt, String context) {
        try {
            ObjectNode root = mapper.createObjectNode();
            root.put("model", "ft:gpt-3.5-turbo-0125:testplatform::CoBT8aLN"); // 実際に利用するモデル名

            ArrayNode messages = mapper.createArrayNode();

            ObjectNode systemMsg = mapper.createObjectNode();
            systemMsg.put("role", "system");
            systemMsg.put("content",
                    "You are a helpful assistant that summarizes Slack threads in bullet points.\n\n"
                            + "Summarize the following messages focusing on:\n"
                            + "- 重要な事実\n"
                            + "- 期限や日時\n"
                            + "- 質問と回答の関係\n"
                            + "- 決定事項\n\n"
                            + "Here are the representative messages:\n"
                            + context
            );
            messages.add(systemMsg);

            ObjectNode userMsg = mapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.put("content", userPrompt);
            messages.add(userMsg);

            root.set("messages", messages);

            String body = mapper.writeValueAsString(root);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(response.body());

            if (json.has("error")) {
                return "OpenAI APIエラー: " + json.get("error").get("message").asText();
            }

            return json.get("choices").get(0).get("message").get("content").asText();

        } catch (Exception e) {
            e.printStackTrace();
            return "要約処理中にエラーが発生しました。";
        }
    }
}
