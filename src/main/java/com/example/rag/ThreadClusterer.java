package com.example.rag;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;

/**
 * EmbeddingベクトルをPythonのHDBSCANスクリプトでクラスタリングし、
 * 各クラスタから代表メッセージを抽出するユーティリティ。
 */
public class ThreadClusterer {

    // 仮想環境のPython実行ファイルパス（環境に合わせて変更）
    private final String pythonPath = "venv\\Scripts\\python"; // Windows例
    private final String scriptPath = "python/cluster.py";

    /**
     * @param embeddings 各メッセージのEmbeddingベクトル (double[] のリスト)
     * @param messages   Embeddingに対応する元メッセージ (同じ順序)
     * @return クラスタごとの代表メッセージ一覧
     */
    public List<String> clusterAndExtractRepresentatives(List<double[]> embeddings, List<String> messages) {
        if (embeddings.isEmpty() || messages.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // JSON入力を構築
            Map<String, Object> payload = new HashMap<>();
            payload.put("embeddings", embeddings);
            payload.put("messages", messages);

            ObjectMapper mapper = new ObjectMapper();
            String jsonInput = mapper.writeValueAsString(payload);

            // Pythonプロセス起動
            ProcessBuilder pb = new ProcessBuilder(pythonPath, scriptPath);
            Process process = pb.start();

            // 標準入力にJSONを渡す
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
                writer.write(jsonInput);
                writer.flush();
            }

            // 標準出力から結果を受け取る
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            // エラーストリームも確認（デバッグ用）
            try (BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String errLine;
                while ((errLine = errReader.readLine()) != null) {
                    System.err.println("[Python Error] " + errLine);
                }
            }

            // プロセス終了待機
            process.waitFor();

            // JSONをパースして代表メッセージを返す
            Map<?, ?> result = mapper.readValue(output.toString(), Map.class);
            List<String> representatives = (List<String>) result.get("representatives");

            // フォールバック処理: 代表が空なら複数基準で選ぶ
            if (representatives == null || representatives.isEmpty()) {
                System.err.println("[Clusterer Warning] No representatives extracted. Applying fallback strategy.");

                List<String> fallback = new ArrayList<>();

                // 1. 最初のメッセージ
                if (!messages.isEmpty()) {
                    fallback.add(messages.get(0));
                }

                // 2. 最後のメッセージ
                if (messages.size() > 1) {
                    fallback.add(messages.get(messages.size() - 1));
                }

                // 3. 最長メッセージ
                String longest = messages.stream()
                        .max(Comparator.comparingInt(String::length))
                        .orElse(null);
                if (longest != null && !fallback.contains(longest)) {
                    fallback.add(longest);
                }

                // 4. 最新3件
                int n = Math.min(3, messages.size());
                List<String> latest = messages.subList(messages.size() - n, messages.size());
                for (String msg : latest) {
                    if (!fallback.contains(msg)) {
                        fallback.add(msg);
                    }
                }

                representatives = fallback;
            }

            return representatives;
        } catch (Exception e) {
            e.printStackTrace();
            // フォールバック処理: エラー時も最新N件を返す
            System.err.println("[Clusterer Error] Clustering failed. Falling back to last 5 messages.");
            List<String> fallback = new ArrayList<>();

            // 1. 最初のメッセージ
            if (!messages.isEmpty()) {
                fallback.add(messages.get(0));
            }

            // 2. 最後のメッセージ
            if (messages.size() > 1) {
                fallback.add(messages.get(messages.size() - 1));
            }

            // 3. 最長メッセージ
            String longest = messages.stream()
                    .max(Comparator.comparingInt(String::length))
                    .orElse(null);
            if (longest != null && !fallback.contains(longest)) {
                fallback.add(longest);
            }

            // 4. 最新3件
            int n = Math.min(3, messages.size());
            List<String> latest = messages.subList(messages.size() - n, messages.size());
            for (String msg : latest) {
                if (!fallback.contains(msg)) {
                    fallback.add(msg);
                }
            }

            return fallback;
        }
    }
}
