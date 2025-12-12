package com.example.repo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import java.util.List;
import com.example.util.Chunk;

public class FaqRepository {
    private final JdbcTemplate jdbcTemplate;

    public FaqRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(String docId, String section, String content, double[] embedding) {
        // double[] を Postgres vector 型に渡すために文字列化
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < embedding.length; i++) {
            sb.append(embedding[i]);
            if (i < embedding.length - 1) sb.append(',');
        }
        sb.append(']');
        String embeddingStr = sb.toString();

        String sql = "INSERT INTO faq_chunks (doc_id, section, content, embedding) VALUES (?, ?, ?, ?::vector)";
        jdbcTemplate.update(sql, docId, section, content, embeddingStr);
    }

    public boolean exists(String docId, String section, String content) {
        String sql = "SELECT COUNT(*) FROM faq_chunks WHERE doc_id = ? AND section = ? AND content = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, docId, section, content);
        return count > 0;
    }

    public List<Chunk> search(double[] embedding, int limit) {
        // embedding を文字列化して Postgres vector 型に渡す
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < embedding.length; i++) {
            sb.append(embedding[i]);
            if (i < embedding.length - 1) sb.append(',');
        }
        sb.append(']');
        String embeddingStr = sb.toString();

        String sql = """
        SELECT doc_id, section, content
        FROM faq_chunks
        ORDER BY embedding <-> ?::vector
        LIMIT ?
        """;

        RowMapper<Chunk> mapper = (rs, rowNum) ->
                new Chunk(
                        rs.getString("doc_id"),
                        rs.getString("section"),
                        rs.getString("content")
                );

        return jdbcTemplate.query(sql, mapper, embeddingStr, limit);
    }
}
