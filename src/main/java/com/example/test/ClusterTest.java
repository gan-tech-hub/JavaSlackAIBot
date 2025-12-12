package com.example.test;

import java.util.*;
import com.example.rag.ThreadClusterer;

public class ClusterTest {
    public static void main(String[] args) {
        ThreadClusterer clusterer = new ThreadClusterer();

        // テスト用の埋め込みベクトル（ダミーデータ）
        List<double[]> embeddings = Arrays.asList(
                // グループ1（近い値）
                new double[]{0.10, 0.20},
                new double[]{0.11, 0.19},
                new double[]{0.12, 0.18},
                new double[]{0.09, 0.21},
                new double[]{0.13, 0.22},
                new double[]{0.14, 0.23},
                new double[]{0.15, 0.25},
                new double[]{0.16, 0.24},
                new double[]{0.17, 0.26},
                new double[]{0.18, 0.27},

                // グループ2（別のクラスタ）
                new double[]{0.80, 0.90},
                new double[]{0.82, 0.88},
                new double[]{0.81, 0.91},
                new double[]{0.83, 0.89},
                new double[]{0.84, 0.92},
                new double[]{0.85, 0.93},
                new double[]{0.86, 0.94},
                new double[]{0.87, 0.95},
                new double[]{0.88, 0.96},
                new double[]{0.89, 0.97},

                // グループ3（さらに別のクラスタ）
                new double[]{0.40, 0.50},
                new double[]{0.41, 0.49},
                new double[]{0.42, 0.48},
                new double[]{0.43, 0.47},
                new double[]{0.44, 0.46},
                new double[]{0.45, 0.55},
                new double[]{0.46, 0.56},
                new double[]{0.47, 0.57},
                new double[]{0.48, 0.58},
                new double[]{0.49, 0.59}
        );

        List<String> messages = Arrays.asList(
                // グループ1
                "メッセージA1","メッセージA2","メッセージA3","メッセージA4","メッセージA5",
                "メッセージA6","メッセージA7","メッセージA8","メッセージA9","メッセージA10",

                // グループ2
                "メッセージB1","メッセージB2","メッセージB3","メッセージB4","メッセージB5",
                "メッセージB6","メッセージB7","メッセージB8","メッセージB9","メッセージB10",

                // グループ3
                "メッセージC1","メッセージC2","メッセージC3","メッセージC4","メッセージC5",
                "メッセージC6","メッセージC7","メッセージC8","メッセージC9","メッセージC10"
        );

        // 実行
        List<String> reps = clusterer.clusterAndExtractRepresentatives(embeddings, messages);
        System.out.println("代表メッセージ: " + reps);
    }
}
