package com.example.slackbot.service;

import com.slack.api.bolt.App;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.model.event.AppMentionEvent;
import com.slack.api.methods.response.conversations.ConversationsRepliesResponse;
import com.example.service.RagService;
import com.example.service.OpenAiClient;
import com.example.service.ReportResult;
import com.example.rag.ThreadClusterer;
import com.slack.api.model.Message;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.concurrent.Executors;
import java.nio.file.Files;
import java.nio.file.Path;

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
            String channelId = payload.getEvent().getChannel();
            log.info("Received AppMentionEvent: userId={} text={}", userId, userMessage);

            // レポート生成コマンド判定
            if (isReportCommand(userMessage)) {
                ReportCommand command = parseReportCommand(userMessage);
                log.info("Detected report command: {}", command);

                // ★ レポート生成処理を呼ぶ
                ctx.say("<@" + userId + "> レポート生成を開始します…\n"
                        + "種類: " + command.getReportType() + "\n"
                        + "期間: " + command.getPeriod());

                // ★ CSV を読み込む（初号機は sales.csv 固定）
                String basePath = System.getenv("REPORT_CSV_PATH");
                if (basePath == null) {
                    ctx.say("環境変数 REPORT_CSV_PATH が設定されていません。");
                    return ctx.ack();
                }

                String csvPath = basePath + "/sales.csv";
                log.info("Loading CSV from: {}", csvPath);

                String csvData = Files.readString(Path.of(csvPath));

                // ★ OpenAI にレポート生成を依頼
                ReportResult result = openAiClient.generateReport(
                        command.getReportType(),
                        command.getPeriod(),
                        csvData
                );
                log.info("OpenAI report result: {}", result.getSummary());

                // ★ Python を実行してグラフ生成
                String outputPath = basePath + "/output.png";
                int exit = runPythonGraph(csvPath, outputPath);

                if (exit != 0) {
                    ctx.say("Python グラフ生成中にエラーが発生しました。");
                    return ctx.ack();
                }

                // ★ Slack に PNG をアップロード
                var uploadResponse = ctx.client().filesUploadV2(r -> r
                        .channel(channelId)
                        .file(new java.io.File(outputPath))
                        .filename("report.png")
                        .initialComment(result.getSummary()) // ← OpenAI の summary を本文として添付
                );

                // エラーチェック
                if (!uploadResponse.isOk()) {
                    log.error("Slack upload error: {}", uploadResponse.getError());
                    ctx.say("Slack への画像アップロードに失敗しました。");
                }

                return ctx.ack();
            }

            String answer = ragService.answer(userMessage);
            ctx.say("<@" + userId + "> さん " + answer);
            log.info("RAG answer={}", answer);

            return ctx.ack();
        });

        // メッセージショートカット: summarize_thread
        app.messageShortcut("summarize_thread", (req, ctx) -> {
            ctx.ack(); // 即ACK

            Executors.newSingleThreadExecutor().submit(() -> {
                try {
                    Message msg = req.getPayload().getMessage();
                    String channelId = req.getPayload().getChannel().getId();
                    String threadTs = msg.getTs();
                    log.info("Shortcut 'summarize_thread' triggered: channelId={} threadTs={}", channelId, threadTs);

                    if (threadTs == null) {
                        log.warn("threadTs is null, cannot fetch replies");
                        ctx.say("親メッセージのTSが取得できませんでした。");
                        return;
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

                    // ★ ctx.say() も try の中に入れる必要がある
                    ctx.say(r -> r.channel(channelId).threadTs(threadTs).text(summary));

                } catch (Exception e) {
                    log.error("Error during summarize_thread processing", e);
                    try {
                        ctx.say("要約処理中にエラーが発生しました。");
                    } catch (Exception ignore) {}
                }
            });

            return ctx.ack();
        });

        SocketModeApp socketModeApp = new SocketModeApp(appToken, app);
        socketModeApp.start();
        log.info("SocketModeApp started successfully");
    }

    private boolean isReportCommand(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        return lower.contains("create report");
    }

    private ReportCommand parseReportCommand(String text) {
        String lower = text.toLowerCase();

        // メンション部分 <@U12345> を削除
        lower = lower.replaceAll("<@[^>]+>", "").trim();

        String reportType = "sales";      // デフォルト
        String period = "this month";     // デフォルト

        int aboutIndex = lower.indexOf("about");
        if (aboutIndex != -1) {
            String afterAbout = lower.substring(aboutIndex + "about".length()).trim();

            int forIndex = afterAbout.indexOf("for");
            if (forIndex != -1) {
                String typePart = afterAbout.substring(0, forIndex).trim();
                String periodPart = afterAbout.substring(forIndex + "for".length()).trim();

                if (!typePart.isEmpty()) reportType = typePart;
                if (!periodPart.isEmpty()) period = periodPart;

            } else {
                if (!afterAbout.isEmpty()) reportType = afterAbout;
            }
        }

        return new ReportCommand(reportType, period);
    }

    private int runPythonGraph(String csvPath, String outputPath) {
        try {
            String pythonScript = System.getenv("PYTHON_SCRIPT_PATH");
            if (pythonScript == null) {
                log.error("環境変数 PYTHON_SCRIPT_PATH が設定されていません");
                return -1;
            }

            ProcessBuilder pb = new ProcessBuilder(
                    "C:\\Users\\gan01\\IdeaProjects\\JavaSlackAIBot\\venv\\Scripts\\python.exe",
                    pythonScript,
                    csvPath,
                    outputPath
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Python の標準出力をログに出す（デバッグ用）
            try (var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[Python] {}", line);
                }
            }

            int exitCode = process.waitFor();
            log.info("Python process finished with exit code {}", exitCode);
            return exitCode;

        } catch (Exception e) {
            log.error("Python 実行中にエラーが発生しました", e);
            return -1;
        }
    }
}

// 追加：レポートコマンド用の DTO
class ReportCommand {
    private final String reportType;
    private final String period;

    public ReportCommand(String reportType, String period) {
        this.reportType = reportType;
        this.period = period;
    }

    public String getReportType() { return reportType; }
    public String getPeriod() { return period; }

    @Override
    public String toString() {
        return "ReportCommand{reportType='" + reportType + "', period='" + period + "'}";
    }
}
