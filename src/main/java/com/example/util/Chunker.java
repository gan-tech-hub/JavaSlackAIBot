package com.example.util;

import java.util.ArrayList;
import java.util.List;

public class Chunker {
    public static List<Chunk> split(String text, String docId) {
        List<Chunk> chunks = new ArrayList<>();
        int chunkSize = 1000, overlap = 200;
        for (int start = 0; start < text.length(); start += (chunkSize - overlap)) {
            int end = Math.min(text.length(), start + chunkSize);
            String content = text.substring(start, end);
            chunks.add(new Chunk(docId, "section-" + start, content));
        }
        return chunks;
    }
}
