package com.example.job;

import com.example.service.OpenAiClient;
import com.example.repo.FaqRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FaqIngestor {
    private final OpenAiClient openAiClient;
    private final FaqRepository faqRepository;
    private String faqPath;

    // Setterを用意（Springの<property>で注入される）
    public void setFaqPath(String faqPath) {
        this.faqPath = faqPath;
    }

    public FaqIngestor(OpenAiClient openAiClient, FaqRepository faqRepository) {
        this.openAiClient = openAiClient;
        this.faqRepository = faqRepository;
    }

    public void ingest() {
        try {
            if (faqPath == null || faqPath.isBlank()) {
                System.err.println("[FaqIngestor] faqPath が設定されていません。処理をスキップします。");
                return;
            }
            Path root = Path.of(faqPath);
            System.out.println("[FaqIngestor] ingest() called with faqPath=" + root.toAbsolutePath());
            ingest(root); // 既存の ingest(Path) を呼ぶ
        } catch (IOException e) {
            throw new RuntimeException("Ingest failed", e);
        }
    }

    // ★ログを追加
    public void ingest(Path root) throws IOException {
        System.out.println("[FaqIngestor] ingest() called with root=" + root.toAbsolutePath());

        Files.walk(root)
                .filter(p -> p.toString().endsWith(".md"))
                .forEach(p -> {
                    System.out.println("[FaqIngestor] Processing file: " + p);
                    try {
                        String doc = Files.readString(p);
                        List<String> chunks = splitFaqIntoChunks(doc, p.getFileName().toString());
                        System.out.println("[FaqIngestor] Split into " + chunks.size() + " chunks");

                        for (String chunk : chunks) {
                            if (faqRepository.exists(p.getFileName().toString(), "FAQ", chunk)) {
                                System.out.println("[FaqIngestor] Skip duplicate chunk for file=" + p.getFileName());
                                continue;
                            }

                            System.out.println("[FaqIngestor] Embedding chunk (length=" + chunk.length() + ")");
                            List<double[]> embList = openAiClient.embed(List.of(chunk));
                            double[] emb = embList.get(0);

                            faqRepository.insert(p.getFileName().toString(), "FAQ", chunk, emb);
                            System.out.println("[FaqIngestor] Inserted chunk into DB for file=" + p.getFileName());
                        }
                    } catch (Exception e) {
                        System.err.println("[FaqIngestor] ERROR during ingestion for file=" + p);
                        e.printStackTrace();
                        throw new RuntimeException("Ingestion failed for " + p, e);
                    }
                });

        System.out.println("[FaqIngestor] ingest() completed for root=" + root.toAbsolutePath());
    }

    /**
     * 質問＋回答単位でチャンク化する
     */
    private List<String> splitFaqIntoChunks(String faqText, String docId) {
        List<String> chunks = new ArrayList<>();
        String[] lines = faqText.split("\n");

        StringBuilder currentChunk = new StringBuilder();
        for (String line : lines) {
            if (line.trim().startsWith("Q:")) {
                // 既存チャンクがあれば保存
                if (!currentChunk.isEmpty()) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk.setLength(0);
                }
            }
            currentChunk.append(line).append("\n");
        }

        // 最後のチャンクを追加
        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }
}
