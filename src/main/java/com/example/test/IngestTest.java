package com.example.test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import java.nio.file.Paths;
import com.example.job.FaqIngestor;  // パッケージは実際の構成に合わせて変更

public class IngestTest {
    public static void main(String[] args) throws Exception {
        // applicationContext.xml を読み込む
        ApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext.xml");

        // FaqIngestor Bean を取得
        FaqIngestor ingestor = ctx.getBean(FaqIngestor.class);

        // data/faq 配下のMarkdownを投入
        ingestor.ingest(Paths.get("data/faq"));

        System.out.println("FAQ ingestion completed!");
    }
}
