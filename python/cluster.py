import sys
import json
import numpy as np
import hdbscan

def main():
    # JavaからJSON文字列を受け取る
    raw = sys.stdin.read()
    payload = json.loads(raw)

    embeddings = np.array(payload["embeddings"])
    messages = payload["messages"]

    # HDBSCANクラスタリング
    clusterer = hdbscan.HDBSCAN(min_cluster_size=5)
    labels = clusterer.fit_predict(embeddings)

    # クラスタごとに代表メッセージを抽出
    representatives = []
    for cluster_id in set(labels):
        if cluster_id == -1:
            continue  # ノイズはスキップ
        indices = np.where(labels == cluster_id)[0]
        centroid = embeddings[indices].mean(axis=0)
        dists = np.linalg.norm(embeddings[indices] - centroid, axis=1)
        rep_index = indices[np.argmin(dists)]
        representatives.append(messages[rep_index])

    # 結果をJSONで返す
    print(json.dumps({"representatives": representatives}, ensure_ascii=False))

if __name__ == "__main__":
    main()
