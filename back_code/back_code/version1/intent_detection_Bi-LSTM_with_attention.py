import torch
import torch.nn as nn
import pandas as pd
import matplotlib.pyplot as plt
from torch.utils.data import Dataset, DataLoader
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import accuracy_score
from transformers import BertTokenizer
import numpy as np
from tqdm import tqdm  # 引入 tqdm 进度条库

# ===================== 数据加载与预处理 =====================
def load_and_preprocess_data(filepath, max_length):
    df = pd.read_csv(filepath)

    texts = df['question'].values
    labels = df['intent_id'].values

    # 标签编码
    label_encoder = LabelEncoder()
    labels = label_encoder.fit_transform(labels)

    # 使用 BertTokenizer 进行分词
    tokenizer = BertTokenizer.from_pretrained('bert-base-chinese')

    # 处理文本分词和填充
    def tokenize_and_pad(text):
        encoded = tokenizer.encode(text, add_special_tokens=True, max_length=max_length, truncation=True)
        return encoded + [0] * (max_length - len(encoded)) if len(encoded) < max_length else encoded

    tokenized_texts = [tokenize_and_pad(text) for text in texts]

    return tokenized_texts, labels, label_encoder

class TextDataset(Dataset):
    def __init__(self, texts, labels):
        self.texts = torch.tensor(texts)
        self.labels = torch.tensor(labels).long()

    def __len__(self):
        return len(self.texts)

    def __getitem__(self, idx):
        return self.texts[idx], self.labels[idx]

# ===================== Bi-LSTM + Attention =====================
class Attention(nn.Module):
    def __init__(self, hidden_dim):
        super(Attention, self).__init__()
        self.attention = nn.Linear(hidden_dim * 2, 1)

    def forward(self, lstm_out):
        attn_weights = torch.softmax(self.attention(lstm_out), dim=1)  # [batch_size, seq_len, 1]
        context = torch.sum(attn_weights * lstm_out, dim=1)  # [batch_size, hidden_dim * 2]
        return context

class BiLSTMWithAttention(nn.Module):
    def __init__(self, vocab_size, embedding_dim, hidden_dim, output_dim, n_layers=2, dropout=0.5):
        super(BiLSTMWithAttention, self).__init__()
        self.embedding = nn.Embedding(vocab_size, embedding_dim)
        self.lstm = nn.LSTM(embedding_dim, hidden_dim, n_layers, bidirectional=True, dropout=dropout, batch_first=True)
        self.attention = Attention(hidden_dim)
        self.fc = nn.Linear(hidden_dim * 2, output_dim)
        self.dropout = nn.Dropout(dropout)

    def forward(self, x):
        embedded = self.embedding(x)
        lstm_out, _ = self.lstm(embedded)  # lstm_out: [batch_size, seq_len, hidden_dim * 2]
        context = self.attention(lstm_out)  # [batch_size, hidden_dim * 2]
        out = self.dropout(context)
        out = self.fc(out)
        return out

# ===================== 训练与评估 =====================
def train_model(model, train_loader, optimizer, criterion, device):
    model.train()
    total_loss, correct, total = 0, 0, 0

    # 使用 tqdm 进度条
    progress_bar = tqdm(train_loader, desc="Training", leave=True)

    for texts, labels in progress_bar:
        texts, labels = texts.to(device), labels.to(device)

        optimizer.zero_grad()
        outputs = model(texts)
        loss = criterion(outputs, labels)
        loss.backward()
        optimizer.step()

        total_loss += loss.item()
        correct += (outputs.argmax(1) == labels).sum().item()
        total += labels.size(0)

        # 更新进度条信息
        progress_bar.set_postfix(loss=loss.item(), accuracy=correct / total)

    return total_loss / len(train_loader), correct / total

def evaluate_model(model, test_loader, criterion, device):
    model.eval()
    total_loss, correct, total = 0, 0, 0

    with torch.no_grad():
        for texts, labels in test_loader:
            texts, labels = texts.to(device), labels.to(device)
            outputs = model(texts)
            loss = criterion(outputs, labels)

            total_loss += loss.item()
            correct += (outputs.argmax(1) == labels).sum().item()
            total += labels.size(0)

    return total_loss / len(test_loader), correct / total

# ===================== 主函数 =====================
if __name__ == "__main__":
    filepath = 'data/intent_detection.csv'
    max_length, batch_size = 50, 32
    embedding_dim, hidden_dim = 128, 128
    n_layers, dropout = 2, 0.5
    num_epochs, learning_rate = 30, 0.001

    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')

    # 数据加载
    tokenized_texts, labels, label_encoder = load_and_preprocess_data(filepath, max_length)
    X_train, X_test, y_train, y_test = train_test_split(tokenized_texts, labels, test_size=0.2, random_state=42)

    train_loader = DataLoader(TextDataset(X_train, y_train), batch_size=batch_size, shuffle=True)
    test_loader = DataLoader(TextDataset(X_test, y_test), batch_size=batch_size, shuffle=False)

    # 模型初始化
    vocab_size = len(BertTokenizer.from_pretrained('bert-base-chinese').vocab)
    output_dim = len(label_encoder.classes_)
    model = BiLSTMWithAttention(vocab_size, embedding_dim, hidden_dim, output_dim, n_layers, dropout).to(device)

    criterion = nn.CrossEntropyLoss()
    optimizer = torch.optim.Adam(model.parameters(), lr=learning_rate)

    # 记录训练过程
    train_losses, train_accs, test_losses, test_accs = [], [], [], []
    train_log = []

    for epoch in range(num_epochs):
        train_loss, train_acc = train_model(model, train_loader, optimizer, criterion, device)
        test_loss, test_acc = evaluate_model(model, test_loader, criterion, device)

        train_losses.append(train_loss)
        train_accs.append(train_acc)
        test_losses.append(test_loss)
        test_accs.append(test_acc)
        train_log.append([epoch + 1, train_loss, train_acc, test_loss, test_acc])

        print(f"Epoch {epoch + 1}/{num_epochs}: Train Loss = {train_loss:.4f}, Train Acc = {train_acc:.4f}, Test Loss = {test_loss:.4f}, Test Acc = {test_acc:.4f}")

    # 保存训练数据到 CSV 文件
    df_log = pd.DataFrame(train_log, columns=["Epoch", "Train Loss", "Train Accuracy", "Test Loss", "Test Accuracy"])
    df_log.to_csv("BiLSTM_Attention_training_log.csv", index=False)
    print("训练数据已保存为 BiLSTM_Attention_training_log.csv")

    # 绘制训练曲线（无标记点）
    plt.figure(figsize=(12, 5))
    plt.subplot(1, 2, 1)
    plt.plot(train_losses, label='Train Loss', linestyle='-')
    plt.plot(test_losses, label='Test Loss', linestyle='-')
    plt.subplot(1, 2, 2)
    plt.plot(train_accs, label='Train Accuracy', linestyle='-')
    plt.plot(test_accs, label='Test Accuracy', linestyle='-')
    plt.tight_layout()
    plt.savefig("bilstm_with_attention_metrics.png")
    plt.show()

    #torch.save(model.state_dict(), 'bilstm_with_attention.pth')
    print("Model saved to 'bilstm_with_attention.pth'")
