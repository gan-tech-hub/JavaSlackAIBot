# **SlackAIBot**  
Java（Slack Bolt）+ OpenAI + Python（pandas/matplotlib）

Slack から **FAQ検索・スレッド要約・パーソナライズ応答・レポート生成** を実行できる、  
**AI × Slack 統合型アシスタント Bot** です。  

社内ナレッジ検索、議事録生成、個人最適化応答、売上レポート作成など、  
実務に直結する AI ワークフローを Slack だけで完結できます。

---

## 🚀 **デプロイURL**  
※本プロジェクトは Slack Socket Mode を採用しており、  
外部公開 URL を必要としないため **デプロイ URL はありません**。  

---

## 🛠️ **機能概要**

### 1. **社内FAQ × RAG (Retrieval-Augmented Generation)**
- Slackで「○○の手順教えて」と聞くと、Botが社内ドキュメント（Markdown）を検索して回答  
- Embedding によるドキュメントのベクトル化  
- ベクトルDB（pgvector / Pinecone / Weaviate）で類似検索  
- OpenAI にコンテキスト付きで質問を投げる  

---

### 2. **Slackスレッド要約Bot**
- Slackのショートカット（例：「スレッド要約」）を実行すると、対象スレッド全体を要約  
- Embedding によるスレッドメッセージのクラスタリング  
- OpenAI の要約モデルで自然なまとめを生成  

---

### 3. **パーソナライズ応答 × Finetuning**
- Bot が「丁寧な説明」「箇条書きの要約」など、ユーザーが望む応答スタイルを再現  
- サンプル対話データを作成し、小モデルを Finetuning  
- 「ステップごとに説明する」などのスタイルを学習  

---

### 4. **AI × Slackワークフロー統合（レポート生成）**
- 「@SlackAIBot create report about sales」  
  → AI が要約＋グラフ生成して Slack に PNG で返す  
- OpenAI で JSON 形式のレポート生成  
- Java で JSON パース → Python（pandas/matplotlib）でグラフ生成  
- Slack API（filesUploadV2）で画像投稿  

---

## 🖥️ **技術スタック**

### Slack Bot  
- Slack Bolt for Java  
- Slack Socket Mode（外部公開 URL 不要）

### AI / NLP  
- OpenAI GPT-4o  
- Embedding（RAG / クラスタリング / 要約）  
- JSON 制御プロンプト  
- Finetuning（小モデル最適化）

### バックエンド（Bot ロジック）  
- Java 21  
- Jackson（JSON パース）  
- ProcessBuilder（Java → Python 連携）

### グラフ生成  
- Python 3  
- pandas  
- matplotlib  

### デプロイ（予定）  
- Render（Web Service / Docker）  
※Socket Mode のため公開 URL は不要

### コード管理  
- Git / GitHub  

---

## ⚙️ **セットアップ方法（ローカル）**

### **1️⃣ リポジトリをクローン**
```bash
git clone https://github.com/gan-tech-hub/slack-ai-bot.git
cd slack-ai-bot
```

---

### **2️⃣ Java 依存関係をインストール（Maven）**
```bash
mvn clean package -DskipTests
```

---

### **3️⃣ Python 依存関係をインストール**
```bash
pip install pandas matplotlib
```

---

### **4️⃣ 環境変数を設定（Slack / OpenAI）**
`.env` または OS の環境変数に設定：

```bash
OPENAI_API_KEY=your_openai_key
SLACK_BOT_TOKEN=xoxb-xxxx
SLACK_APP_TOKEN=xapp-xxxx
SLACK_SIGNING_SECRET=xxxx
BASE_PATH=./data
```

---

### **5️⃣ ローカル起動**
```bash
java -jar target/slack-ai-bot.jar
```

Slack で Bot をメンションすると動作します。

---

## 📝 ライセンス
MIT

---

## 👤 作成者  
* 桜庭祐斗  

[GitHub - gan-tech-hub](https://github.com/gan-tech-hub)

---

## 📷 スクリーンショット例 (準備中)  
（Slack での実際の動作例を掲載予定）

- FAQ検索の回答例  
- スレッド要約の返信例  
- パーソナライズ応答例  
- 売上レポート（PNG グラフ）投稿例  
