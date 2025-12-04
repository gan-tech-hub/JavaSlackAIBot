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

    public FaqIngestor(OpenAiClient openAiClient, FaqRepository faqRepository) {
        this.openAiClient = openAiClient;
        this.faqRepository = faqRepository;
    }

    public void ingest(Path root) throws IOException {
        Files.walk(root)
                .filter(p -> p.toString().endsWith(".md"))
                .forEach(p -> {
                    try {
                        String doc = Files.readString(p);
                        List<String> chunks = splitFaqIntoChunks(doc, p.getFileName().toString());

                        for (String chunk : chunks) {
                            double[] emb = openAiClient.embed(chunk);
                            // docId と section は簡略化し、ファイル名を docId として利用
                            faqRepository.insert(p.getFileName().toString(), "FAQ", chunk, emb);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Ingestion failed for " + p, e);
                    }
                });
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
