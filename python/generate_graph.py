# python/generate_graph.py

import sys
import pandas as pd
import matplotlib.pyplot as plt

def main():
    csv_path = sys.argv[1]
    output_path = sys.argv[2]

    df = pd.read_csv(csv_path)

    plt.figure(figsize=(8, 4))
    plt.plot(df['month'], df['sales'], marker='o')
    plt.title('Sales Trend')
    plt.xlabel('Month')
    plt.ylabel('Sales')
    plt.grid(True)

    plt.tight_layout()
    plt.savefig(output_path)

if __name__ == "__main__":
    main()
