package com.example;

import com.example.job.FaqIngestor;
import com.example.slackbot.service.SlackService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.nio.file.Paths;

public class AppInitializer {
    public static void main(String[] args) throws Exception {
        // Springコンテキストを起動
        ApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext.xml");

        // FAQ再投入（起動時に必ず走る）
        FaqIngestor faqIngestor = ctx.getBean(FaqIngestor.class);
        faqIngestor.ingest(Paths.get("data/faq"));

        // Slack Bot起動
        SlackService slackService = ctx.getBean(SlackService.class);
        slackService.start();
    }
}
