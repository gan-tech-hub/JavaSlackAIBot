package com.example.slackbot.controller;

import com.example.slackbot.service.SlackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class SlackController {

    private final SlackService slackService;

    @Autowired
    public SlackController(SlackService slackService) {
        this.slackService = slackService;
    }

    // 旧 handleEvent は削除
    // SlackService.start() を呼ぶだけにする
    public void runBot() throws Exception {
        slackService.start();
    }
}

/* 旧SlackService対応
package com.example.slackbot.controller;

import com.example.slackbot.util.SlackSignatureVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.example.slackbot.service.SlackService;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

@Controller
@RequestMapping("/slack")
public class SlackController {

    @Autowired
    private SlackService slackService;

    private static final Logger log = LoggerFactory.getLogger(SlackController.class);
    private final ObjectMapper mapper = new ObjectMapper();

    // 環境変数から取得（IntelliJのRun/Debug ConfigやOS環境に設定）
    private final String signingSecret = System.getenv("SLACK_SIGNING_SECRET");
    private final String botToken = System.getenv("SLACK_BOT_TOKEN");

    @PostMapping(value = "/events", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String handleEvents(@RequestBody String body, HttpServletRequest request) throws IOException {
        // 1) URL検証: challengeがあればそのまま返す
        JsonNode json = mapper.readTree(body);
        if ("url_verification".equals(json.path("type").asText()) && json.has("challenge")) {
            String challenge = json.get("challenge").asText();
            log.info("Slack URL verification challenge received: {}", challenge);
            return challenge;
        }

        // 2) 署名検証
        String timestamp = request.getHeader("X-Slack-Request-Timestamp");
        String signature = request.getHeader("X-Slack-Signature");
        if (signingSecret == null || !SlackSignatureVerifier.isValid(signingSecret, timestamp, body, signature)) {
            log.warn("Invalid Slack signature. Rejecting request.");
            return "invalid signature";
        }

        // 3) リトライ処理の可能性に備え、headersでretry確認（必要ならidempotency対応）
        String retryNum = request.getHeader("X-Slack-Retry-Num");
        String retryReason = request.getHeader("X-Slack-Retry-Reason");
        if (retryNum != null) {
            log.info("Slack retry received: num={}, reason={}", retryNum, retryReason);
            // 実運用ではリトライ重複処理を防ぐためにイベントIDで重複排除する（次ステップで対応）
        }

        // 4) メッセージイベントのみ処理する
        String eventType = json.path("event").path("type").asText();
        if (!"message".equals(eventType) && !"app_mention".equals(eventType)) {
            log.info("Ignoring non-message event: {}", eventType);
            return "ok";
        }

        // 5) ボット自身のメッセージやサブタイプを無視
        String subtype = json.path("event").path("subtype").asText(null);
        String botId = json.path("event").path("bot_id").asText(null);
        if (subtype != null || botId != null) {
            log.info("Ignoring bot or subtype message. subtype={}, botId={}", subtype, botId);
            return "ok";
        }

        // 6) 必要情報の抽出
        String text = json.path("event").path("text").asText("");
        String channel = json.path("event").path("channel").asText("");
        String user = json.path("event").path("user").asText("");

        log.info("Received message from user={} in channel={}: {}", user, channel, text);

        // 7) SlackServiceに処理を委譲（受信内容を組み込んで返信）
        //    ここは後彼修正！OpenAI連携部分！
        //
        slackService.handleEvent(eventType, json.path("event"));

        return "ok";
    }
}
*/