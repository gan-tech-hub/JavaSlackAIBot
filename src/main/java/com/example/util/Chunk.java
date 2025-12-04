package com.example.util;

public class Chunk {
    private final String docId;
    private final String section;
    private final String content;

    public Chunk(String docId, String section, String content) {
        this.docId = docId;
        this.section = section;
        this.content = content;
    }
    public String docId() { return docId; }
    public String section() { return section; }
    public String content() { return content; }
}
