package com.example.slackbot.service;

import com.slack.api.bolt.App;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.model.event.AppMentionEvent;
import com.slack.api.methods.response.conversations.ConversationsRepliesResponse;
import com.example.service.RagService;
import com.example.service.OpenAiClient;
import com.example.rag.ThreadClusterer;
import com.slack.api.model.Message;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

/*--- デバッグ ---*/
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlackService {
    /*--- デバッグ ---*/
    private static final Logger log = LoggerFactory.getLogger(SlackService.class);

    private final String appToken;   // App Token (xapp-...)
    private final RagService ragService;
    private final OpenAiClient openAiClient;
    private final Set<String> processedEvents = ConcurrentHashMap.newKeySet();

    public SlackService(String appToken, RagService ragService, OpenAiClient openAiClient) {
        this.appToken = appToken;
        this.ragService = ragService;
        this.openAiClient = openAiClient;
        log.info("SlackService initialized with appToken={}", appToken != null ? "SET" : "NULL");
    }

    public void start() throws Exception {
        App app = new App();

        // メンションイベント
        app.event(AppMentionEvent.class, (payload, ctx) -> {
            String eventTs = payload.getEvent().getTs();
            if (processedEvents.contains(eventTs)) {
                log.info("Skip duplicate event: {}", eventTs);
                return ctx.ack();
            }
            processedEvents.add(eventTs);

            String userMessage = payload.getEvent().getText();
            String userId = payload.getEvent().getUser();
            log.info("Received AppMentionEvent: userId={} text={}", userId, userMessage);

            String answer = ragService.answer(userMessage);
            ctx.say("<@" + userId + "> さん " + answer);
            log.info("RAG answer={}", answer);

            return ctx.ack();
        });

        // メッセージショートカット: summarize_thread
        app.messageShortcut("summarize_thread", (req, ctx) -> {
            Message msg = req.getPayload().getMessage();
            String channelId = req.getPayload().getChannel().getId();
            String threadTs = msg.getTs();
            log.info("Shortcut 'summarize_thread' triggered: channelId={} threadTs={}", channelId, threadTs);

            if (threadTs == null) {
                log.warn("threadTs is null, cannot fetch replies");
                ctx.say("親メッセージのTSが取得できませんでした。");
                return ctx.ack();
            }

            ConversationsRepliesResponse replies = ctx.client().conversationsReplies(r -> r
                    .channel(channelId)
                    .ts(threadTs)
            );
            log.info("Fetched {} messages from thread", replies.getMessages().size());

            List<String> messages = replies.getMessages().stream()
                    .map(m -> m.getText())
                    .toList();
            log.info("Extracted {} text messages", messages.size());

            List<double[]> embeddings = openAiClient.embed(messages);
            log.info("Generated {} embeddings", embeddings.size());

            ThreadClusterer clusterer = new ThreadClusterer();
            List<String> representatives = clusterer.clusterAndExtractRepresentatives(embeddings, messages);
            log.info("Extracted {} representative messages", representatives.size());

            String summary = openAiClient.summarize(representatives);
            log.info("Summary result: {}", summary);

            ctx.say(r -> r.channel(channelId).threadTs(threadTs).text(summary));
            return ctx.ack();
        });

        SocketModeApp socketModeApp = new SocketModeApp(appToken, app);
        socketModeApp.start();
        log.info("SocketModeApp started successfully");
    }
}
