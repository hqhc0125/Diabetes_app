import os
import pandas as pd
import torch
from torch.utils.data import DataLoader, Dataset
from transformers import ElectraTokenizer, ElectraForSequenceClassification
from sklearn.model_selection import train_test_split
from torch.utils.tensorboard import SummaryWriter
from torch.optim import AdamW
from tqdm import tqdm  # 实时显示进度

# 数据路径
data_path = "E:/Mylife/app/back_code/back_code/version1/data/intent_detection.csv"

# 加载数据
df = pd.read_csv(data_path)
# 如有需要，请确保列名与CSV内一致，例如若标签存储为字符串，请先转换为 int
# 例如：df["intent_id"] = df["intent_id"].astype(int)
data = df[["question", "intent_id"]].rename(columns={"question": "text", "intent_id": "label"})

# 数据集定义
class IntentDataset(Dataset):
    def __init__(self, texts, labels, tokenizer, max_length=128):
        self.texts = texts
        self.labels = labels
        self.tokenizer = tokenizer
        self.max_length = max_length

    def __len__(self):
        return len(self.texts)

    def __getitem__(self, idx):
        text = str(self.texts[idx])
        label = self.labels[idx]
        encoding = self.tokenizer.encode_plus(
            text,
            add_special_tokens=True,
            max_length=self.max_length,
            padding="max_length",
            truncation=True,
            return_tensors="pt"
        )
        return {
            "input_ids": encoding["input_ids"].flatten(),
            "attention_mask": encoding["attention_mask"].flatten(),
            "labels": torch.tensor(label, dtype=torch.long)
        }

# 加载tokenizer和模型
model_path = "E:/Mylife/app/back_code/back_code/version2/chinese-electra-large-generator"  # 使用指定路径
tokenizer = ElectraTokenizer.from_pretrained(model_path)
model = ElectraForSequenceClassification.from_pretrained(
    model_path, num_labels=len(data['label'].unique())
)

# 数据划分
train_texts, val_texts, train_labels, val_labels = train_test_split(
    data['text'].values, data['label'].values, test_size=0.2
)

# 创建DataLoader
train_dataset = IntentDataset(train_texts, train_labels, tokenizer)
val_dataset = IntentDataset(val_texts, val_labels, tokenizer)

train_loader = DataLoader(train_dataset, batch_size=16, shuffle=True)
val_loader = DataLoader(val_dataset, batch_size=16)

# 设置优化器
optimizer = AdamW(model.parameters(), lr=2e-5)

# 设置TensorBoard日志
writer = SummaryWriter()

# 训练函数（添加 tqdm 进度条）
def train_epoch(model, data_loader, optimizer, device):
    model.train()
    losses = []
    correct_predictions = 0
    # 使用 tqdm 包裹 data_loader，显示实时进度
    pbar = tqdm(data_loader, desc="训练进度", leave=False)
    for batch in pbar:
        optimizer.zero_grad()

        input_ids = batch["input_ids"].to(device)
        attention_mask = batch["attention_mask"].to(device)
        labels = batch["labels"].to(device)

        outputs = model(input_ids=input_ids, attention_mask=attention_mask, labels=labels)
        loss = outputs.loss
        logits = outputs.logits

        loss.backward()
        optimizer.step()

        losses.append(loss.item())
        preds = torch.argmax(logits, dim=1)
        correct_predictions += torch.sum(preds == labels)

        # 更新进度条显示当前 batch 的 loss
        pbar.set_postfix({'batch_loss': f'{loss.item():.4f}'})

    accuracy = correct_predictions.double() / len(data_loader.dataset)
    epoch_loss = sum(losses) / len(losses)
    return accuracy, epoch_loss

# 验证函数（使用 tqdm）
def eval_epoch(model, data_loader, device):
    model.eval()
    losses = []
    correct_predictions = 0
    pbar = tqdm(data_loader, desc="验证进度", leave=False)
    with torch.no_grad():
        for batch in pbar:
            input_ids = batch["input_ids"].to(device)
            attention_mask = batch["attention_mask"].to(device)
            labels = batch["labels"].to(device)

            outputs = model(input_ids=input_ids, attention_mask=attention_mask, labels=labels)
            loss = outputs.loss
            logits = outputs.logits

            losses.append(loss.item())
            preds = torch.argmax(logits, dim=1)
            correct_predictions += torch.sum(preds == labels)

            pbar.set_postfix({'batch_loss': f'{loss.item():.4f}'})
    accuracy = correct_predictions.double() / len(data_loader.dataset)
    epoch_loss = sum(losses) / len(losses)
    return accuracy, epoch_loss

# 设置设备
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
model = model.to(device)

# 训练过程
epochs = 30
best_accuracy = 0

for epoch in range(epochs):
    print(f"Epoch {epoch + 1}/{epochs} ...")
    train_accuracy, train_loss = train_epoch(model, train_loader, optimizer, device)
    val_accuracy, val_loss = eval_epoch(model, val_loader, device)

    print(f"Train Accuracy: {train_accuracy:.4f}, Train Loss: {train_loss:.4f}")
    print(f"Val Accuracy: {val_accuracy:.4f}, Val Loss: {val_loss:.4f}")

    # 记录TensorBoard数据
    writer.add_scalar('Train Accuracy', train_accuracy, epoch)
    writer.add_scalar('Train Loss', train_loss, epoch)
    writer.add_scalar('Validation Accuracy', val_accuracy, epoch)
    writer.add_scalar('Validation Loss', val_loss, epoch)

    # 保存最佳模型
    if val_accuracy > best_accuracy:
        best_accuracy = val_accuracy
        print(f"Best Validation Accuracy: {best_accuracy:.4f} (模型已保存)")
        model.save_pretrained("./saved_model_electra")
        tokenizer.save_pretrained("./saved_model_electra")

# 关闭TensorBoard
writer.close()

# 模型预测函数
def predict(text, model, tokenizer, device):
    model.eval()
    encoding = tokenizer.encode_plus(
        text,
        add_special_tokens=True,
        max_length=128,
        padding="max_length",
        truncation=True,
        return_tensors="pt"
    )
    input_ids = encoding["input_ids"].to(device)
    attention_mask = encoding["attention_mask"].to(device)

    with torch.no_grad():
        outputs = model(input_ids=input_ids, attention_mask=attention_mask)
        logits = outputs.logits
        preds = torch.argmax(logits, dim=1).item()
    return preds

# 启动 while 循环进行预测
while True:
    text = input("请输入文本进行预测（输入 'end' 退出）：")
    if text.lower() == "end":
        print("退出预测程序。")
        break

    intent = predict(text, model, tokenizer, device)
    print(f"预测的意图ID：{intent}")
