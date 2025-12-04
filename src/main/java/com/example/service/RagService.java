package com.example.service;

import com.example.repo.FaqRepository;
import com.example.util.Chunk;
import java.util.List;

public class RagService {
    private final OpenAiClient openAiClient;
    private final FaqRepository faqRepository;

    public RagService(OpenAiClient openAiClient, FaqRepository faqRepository) {
        this.openAiClient = openAiClient;
        this.faqRepository = faqRepository;
    }

    public String answer(String question) {
        // 1. 質問をEmbedding
        double[] queryEmbedding = openAiClient.embed(question);

        // 2. Supabase検索（上位3件）
        List<Chunk> candidates = faqRepository.search(queryEmbedding, 3);

        // 3. コンテキストを組み立て
        StringBuilder context = new StringBuilder();
        for (Chunk c : candidates) {
            context.append(c.content()).append("\n");
        }

        // 4. OpenAIに質問＋コンテキストを渡して回答生成
        return openAiClient.chat(
                "以下のFAQを参考に質問に答えてください:\n" + context,
                question
        );
    }
}
