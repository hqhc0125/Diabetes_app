---
pipeline_tag: text-classification
---


# 概述
本模型是基于 chinese-electra-large-generator 预训练模型进行微调得到的，专注于中文糖尿病领域问答任务，旨在为用户提供准确且高效的糖尿病相关问题意图的识别。

# 数据集
我们使用了 DaCorp 数据集进行训练，该数据集来源于中文糖尿病问题分类体系及标注语料库。它涵盖了 6 个大类和 23 个细类，构建了一个包含 122732 个问答对的丰富中文糖尿病问答语料库。此外，其中 8000 个糖尿病问题经过了人工精细标注，形成了一个细粒度的糖尿病标注数据集，为模型的训练提供了高质量且具有针对性的数据支持，使得模型能够更好地理解和回答糖尿病相关的各类问题。

| ID | 大类         | 细类               | 数量  |
|----|:------------|:-------------------|:------|
| 0  | A：诊断     | A0：其他           | 16    |
| 1  | A：诊断     | A1：interpretation of clinical（临床解释） | 344 |
| 2  | A：诊断     | A2：症状/表现       | 292   |
| 3  | A：诊断     | A3：test（检查）     | 65    |
| 4  | B：治疗     | B0：其他           | 388   |
| 5  | B：治疗     | B1：how to use drug（如何用药） | 101 |
| 6  | B：治疗     | B2：drug choice（药物选择） | 514 |
| 7  | B：治疗     | B3：药物不良反应（药物副作用） | 69 |
| 8  | B：治疗     | B4：药物禁忌         | 4     |
| 9  | B：治疗     | B5：Other Therapy（其他治疗方法） | 162 |
| 10 | B：治疗     | B6：Treatment Seeking（寻求治疗） | 788 |
| 11 | C：常识     | C0：other（其他）     | 527   |
| 12 | C：常识     | C1：定义             | 152   |
| 13 | C：常识     | C2：病因学           | 860   |
| 14 | C：常识     | C3：Fertility（生育力） | 67 |
| 15 | C：常识     | C4：Hereditary（遗传性） | 49 |
| 16 | D：健康生活方式 | D0：other（其他）       | 43 |
| 17 | D：健康生活方式 | D1：饮食               | 2126 |
| 18 | D：健康生活方式 | D2：Exercise（锻炼）     | 50 |
| 19 | D：健康生活方式 | D3：weight-losing（减肥） | 7 |
| 20 | E：流行病学 | E1：感染（传染性）     | 31 |
| 21 | E：流行病学 | E2：Prevention（预防） | 34 |
| 22 | E：流行病学 | E3：并发症           | 742   |
| 23 | F：其他     | -                  | 569   |

# 模型
本模型以`chinese-electra-large-generator`作为基础架构，在此基础上进行了微调。通过对 DaCorp 数据集中的问句与意图进行训练，模型学习到了糖尿病领域问答的特定模式和语义信息，从而能够准确地对输入的糖尿病相关问题进行理解，为用户提供专业的糖尿病问句意图识别服务。

# 用法
## 加载模型
    # 加载模型和分词器
    model_path = "chinese-electra-large-Diabetes-question-intent"
    tokenizer = AutoTokenizer.from_pretrained(model_path)
    model = AutoModelForSequenceClassification.from_pretrained(model_path).to(torch.device("cuda" if torch.cuda.is_available() else "cpu"))
## 进行预测
    import torch
    from transformers import AutoTokenizer, AutoModelForSequenceClassification
    
    # 加载模型和分词器
    model_path = "chinese-electra-large-Diabetes-question-intent"
    tokenizer = AutoTokenizer.from_pretrained(model_path)
    model = AutoModelForSequenceClassification.from_pretrained(model_path).to(torch.device("cuda" if torch.cuda.is_available() else "cpu"))
    
    # 定义预测函数
    def predict(text):
        model.eval()
        encoding = tokenizer.encode_plus(
            text,
            add_special_tokens=True,
            max_length=128,
            padding="max_length",
            truncation=True,
            return_tensors="pt"
        )
        input_ids = encoding["input_ids"].to(model.device)
        attention_mask = encoding["attention_mask"].to(model.device)
    
        with torch.no_grad():
            outputs = model(input_ids=input_ids, attention_mask=attention_mask)
            logits = outputs.logits
            preds = torch.argmax(logits, dim=1).item()
    
        return preds
    
    # 预测
    while True:
        text = input("请输入文本进行预测（输入 'end' 退出）：")
        if text.lower() == "end":
            print("退出预测程序。")
            break
    
        intent = predict(text)
        print(f"预测的意图ID：{intent}")