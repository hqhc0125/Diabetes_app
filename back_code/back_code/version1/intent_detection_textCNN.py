import torch
import torch.nn as nn
import torch.optim as optim
from safetensors.torch import save_model
from torch.utils.data import DataLoader, Dataset
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import accuracy_score
import pandas as pd
import numpy as np
import os
from tqdm import tqdm  # 进度条库
import matplotlib.pyplot as plt


# 加载数据集
def load_dataset(file_path):
    data = pd.read_csv(file_path)
    texts = data['question'].tolist()
    labels = data['intent'].tolist()
    return texts, labels


# 数据预处理
class TextDataset(Dataset):
    def __init__(self, texts, labels, tokenizer, label_encoder, max_len):
        self.texts = texts
        self.labels = label_encoder.transform(labels).astype(np.int64)  # 转为int64
        self.tokenizer = tokenizer
        self.max_len = max_len

    def __len__(self):
        return len(self.texts)

    def __getitem__(self, idx):
        tokens = self.tokenizer(self.texts[idx])
        if len(tokens) < self.max_len:
            tokens += [0] * (self.max_len - len(tokens))
        else:
            tokens = tokens[:self.max_len]
        return torch.tensor(tokens), torch.tensor(self.labels[idx], dtype=torch.long)


# 简单的分词器
def simple_tokenizer(text):
    vocab = {word: idx + 1 for idx, word in enumerate(set("".join(texts)))}
    return [vocab[char] for char in text if char in vocab]


# TextCNN模型
class TextCNN(nn.Module):
    def __init__(self, vocab_size, embed_dim, num_classes, kernel_sizes, num_filters):
        super(TextCNN, self).__init__()
        self.embedding = nn.Embedding(vocab_size, embed_dim)
        self.convs = nn.ModuleList([
            nn.Conv2d(1, num_filters, (k, embed_dim)) for k in kernel_sizes
        ])
        self.fc = nn.Linear(num_filters * len(kernel_sizes), num_classes)
        self.dropout = nn.Dropout(0.5)

    def forward(self, x):
        x = self.embedding(x).unsqueeze(1)
        x = [torch.relu(conv(x)).squeeze(3) for conv in self.convs]
        x = [torch.max_pool1d(i, i.size(2)).squeeze(2) for i in x]
        x = torch.cat(x, 1)
        x = self.dropout(x)
        x = self.fc(x)
        return x


# 训练函数（添加 tqdm 进度条）
def train_epoch(model, dataloader, optimizer, criterion):
    model.train()
    total_loss = 0
    all_preds = []
    all_targets = []

    progress_bar = tqdm(dataloader, desc="Training", leave=True)

    for inputs, targets in progress_bar:
        optimizer.zero_grad()
        outputs = model(inputs)
        loss = criterion(outputs, targets)
        loss.backward()
        optimizer.step()

        total_loss += loss.item()
        preds = torch.argmax(outputs, dim=1)
        all_preds.extend(preds.tolist())
        all_targets.extend(targets.tolist())

        progress_bar.set_postfix(loss=loss.item(), accuracy=accuracy_score(all_targets, all_preds))

    accuracy = accuracy_score(all_targets, all_preds)
    return total_loss / len(dataloader), accuracy


# 评估函数（添加 tqdm 进度条）
def evaluate(model, dataloader, criterion):
    model.eval()
    total_loss = 0
    all_preds = []
    all_targets = []

    progress_bar = tqdm(dataloader, desc="Validation", leave=True)

    with torch.no_grad():
        for inputs, targets in progress_bar:
            outputs = model(inputs)
            loss = criterion(outputs, targets)

            total_loss += loss.item()
            preds = torch.argmax(outputs, dim=1)
            all_preds.extend(preds.tolist())
            all_targets.extend(targets.tolist())

            progress_bar.set_postfix(loss=loss.item(), accuracy=accuracy_score(all_targets, all_preds))

    accuracy = accuracy_score(all_targets, all_preds)
    return total_loss / len(dataloader), accuracy


if __name__ == "__main__":
    file_path = "data/intent_detection.csv"
    texts, labels = load_dataset(file_path)

    label_encoder = LabelEncoder()
    label_encoder.fit(labels)

    max_len = 20
    tokenizer = simple_tokenizer
    train_texts, val_texts, train_labels, val_labels = train_test_split(texts, labels, test_size=0.2, random_state=42)
    train_dataset = TextDataset(train_texts, train_labels, tokenizer, label_encoder, max_len)
    val_dataset = TextDataset(val_texts, val_labels, tokenizer, label_encoder, max_len)
    train_loader = DataLoader(train_dataset, batch_size=16, shuffle=True)
    val_loader = DataLoader(val_dataset, batch_size=16, shuffle=False)

    vocab_size = len(set("".join(texts))) + 1
    embed_dim = 50
    num_classes = len(label_encoder.classes_)
    kernel_sizes = [3, 4, 5]
    num_filters = 16

    model = TextCNN(vocab_size, embed_dim, num_classes, kernel_sizes, num_filters)
    criterion = nn.CrossEntropyLoss()
    optimizer = optim.Adam(model.parameters(), lr=0.001)

    epochs = 30
    model_save_path = "textcnn_model.pth"
    csv_filename = "TextCNN_training_log.csv"

    # 记录训练数据到一个 CSV 文件
    train_log = []

    for epoch in range(epochs):
        train_loss, train_accuracy = train_epoch(model, train_loader, optimizer, criterion)
        val_loss, val_accuracy = evaluate(model, val_loader, criterion)

        train_log.append([epoch + 1, train_loss, train_accuracy, val_loss, val_accuracy])
        print(
            f"Epoch {epoch + 1}/{epochs}: Train Loss = {train_loss:.4f}, Train Acc = {train_accuracy:.4f}, Val Loss = {val_loss:.4f}, Val Acc = {val_accuracy:.4f}")

    # 保存训练数据到 CSV 文件
    df_log = pd.DataFrame(train_log,
                          columns=["Epoch", "Train Loss", "Train Accuracy", "Validation Loss", "Validation Accuracy"])
    df_log.to_csv(csv_filename, index=False)
    print(f"训练数据已保存为 {csv_filename}")

    # 保存模型
    #save_model(model, model_save_path)
    #print(f"Model saved to {model_save_path}")
