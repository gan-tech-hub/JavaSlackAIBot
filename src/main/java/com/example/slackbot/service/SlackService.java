package com.example.slackbot.service;

import com.slack.api.bolt.App;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.model.event.AppMentionEvent;
import com.example.service.RagService;

public class SlackService {
    private final String appToken;   // App Token (xapp-...)
    private final RagService ragService;

    public SlackService(String appToken, RagService ragService) {
        this.appToken = appToken;
        this.ragService = ragService;
    }

    public void start() throws Exception {
        App app = new App();

        // メッセージイベントをハンドリング
        app.event(AppMentionEvent.class, (payload, ctx) -> {
            String userMessage = payload.getEvent().getText();
            String userId = payload.getEvent().getUser(); // 送信者のユーザーID

            // RagServiceで回答生成
            String answer = ragService.answer(userMessage);

            // 送信者にメンションして返答
            ctx.say("<@" + userId + "> さん " + answer);

            return ctx.ack();
        });

        // Socket Modeで起動（App Tokenを使用）
        SocketModeApp socketModeApp = new SocketModeApp(appToken, app);
        socketModeApp.start();
    }
}

/* メッセージの単純な送受信
package com.example.slackbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;   // ← これが必要！

import java.nio.charset.StandardCharsets;

@Service
public class SlackService {

    private static final Logger log = LoggerFactory.getLogger(SlackService.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${slack.bot.token}")
    private String botToken;

    public void postMessage(String channel, String text) {
        if (botToken == null) {
            log.error("SLACK_BOT_TOKEN is not set in environment variables.");
            return;
        }

        String url = "https://slack.com/api/chat.postMessage";

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            post.addHeader("Authorization", "Bearer " + botToken);
            post.addHeader("Content-Type", "application/json; charset=utf-8");

            String payload = mapper.createObjectNode()
                    .put("channel", channel)
                    .put("text", text)
                    .toString();

            post.setEntity(new StringEntity(payload, StandardCharsets.UTF_8));

            var response = client.execute(post);
            int statusCode = response.getCode();
            log.info("Slack API response status: {}", statusCode);

            if (statusCode != 200) {
                log.error("Failed to post message to Slack. Status: {}", statusCode);
            }
        } catch (Exception e) {
            log.error("Error posting message to Slack", e);
        }
    }

    public void handleEvent(String eventType, com.fasterxml.jackson.databind.JsonNode event) {
        switch (eventType) {
            case "message":
                // Bot自身の発言は無限ループ防止のためスキップ
                if (event.has("bot_id")) return;

                String text = event.path("text").asText();
                String channel = event.path("channel").asText();
                String user = event.path("user").asText();

                // メンションが含まれている場合はスキップ（app_mentionで処理する）
                if (text.contains("<@" )) {
                    log.info("Skipping message event because it contains a mention: {}", text);
                    return;
                }

                log.info("Message received: user={}, channel={}, text={}", user, channel, text);

                // 受信内容を組み込んだ返信
                postMessage(channel, "こんにちは！「" + text + "」と受信しました！");
                break;

            case "app_mention":
                String mentionText = event.path("text").asText();
                String mentionChannel = event.path("channel").asText();

                log.info("App mention: channel={}, text={}", mentionChannel, mentionText);

                // メンション内容を組み込んだ返信
                postMessage(mentionChannel, "こんにちは！「" + mentionText + "」と受信しました！");
                break;

            default:
                log.info("Unhandled eventType={}", eventType);
        }
    }
}
*/