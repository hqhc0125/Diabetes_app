import os
import pickle
import traceback
import logging
logging.getLogger("streamlit").setLevel(logging.ERROR)

from dotenv import load_dotenv
from flask import Flask, request, jsonify
from entity import extract_entities
from intent_detection import predict
from Web_KGshow import neo4j_query, KG_load
from RAG import RAG_prepare, retrieve_answer
#from langchain_glm import ask_question
from sentence_transformers import SentenceTransformer

load_dotenv(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".env"))

app = Flask(__name__)

# PDF 文件路径（根据实际情况调整）
pdf_path = "E:/Mylife/app/back_code/back_code/version2/data/国家基层糖尿病防治管理指南（ 2022）.pdf"
# 缓存文件路径
cache_file = "./knowledge_base_cache.pkl"

# 初始化嵌入模型
embedding_model = SentenceTransformer(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "multi-qa-MiniLM-L6-cos-v1"),
    device="cpu"
)

# 检查是否存在缓存
if os.path.exists(cache_file):
    with open(cache_file, "rb") as f:
        pdf_content, knowledge_base = pickle.load(f)
    print(f"加载缓存成功：PDF提取 {len(pdf_content)} 页，知识库包含 {len(knowledge_base)} 条知识项。")
else:
    pdf_content, knowledge_base = RAG_prepare(pdf_path, embedding_model)
    print(f"PDF 提取文本完成，共 {len(pdf_content)} 页")
    print(f"知识库构建完成，共包含 {len(knowledge_base)} 条知识项")
    # 将结果缓存到本地文件
    with open(cache_file, "wb") as f:
        pickle.dump((pdf_content, knowledge_base), f)

# 初始化 Neo4j 图谱连接
graph = KG_load()

@app.route('/')
def home():
    return jsonify({
        "status": "running",
        "message": "Diabetes QA API is working",
        "endpoints": {
            "/api/chat": "POST {question: 'your question'}"
        }
    })

@app.route('/api/chat', methods=['POST'])
def chat():
    try:
        data = request.get_json()
        if not data or 'question' not in data:
            return jsonify({"error": "Missing 'question' field"}), 400

        question = data['question']
        final_answer = generate_final_answer(question)
        return jsonify({
            "success": True,
            "question": question,
            "final_answer": final_answer
        })

    except Exception as e:
        traceback.print_exc()
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500

def generate_final_answer(query):
    """
    整合各模块生成最终回答：
    1. 实体识别
    2. 意图检测
    3. 知识图谱查询
    4. RAG 检索
    5. 使用大模型生成最终回答
    """
    # 1. 实体识别
    entities = [entity[0] for entity in extract_entities([query])[0]]

    # 2. 意图检测
    intent_model_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "chinese-electra-large-Diabetes-question-intent")
    intent = predict(query, intent_model_path)

    # 3. 知识图谱查询（示例，用完整的映射你可以自行替换）
    disease_relations = {
        "A0": ["Symptom_Disease", "Test_Disease"],
        "A1": ["Symptom_Disease", "Anatomy_Disease", "Reason_Disease", "Pathogenesis_Disease"],
        "A2": ["Symptom_Disease", "Reason_Disease", "Pathogenesis_Disease", "Class_Disease"],
        "A3": ["Test_Disease", "Treatment_Disease", "Drug_Disease", "Test_Items_Disease"],
        "B0": ["Duration_Drug", "Test_Items_Disease"],
        "B1": ["Frequency_Drug", "Duration_Drug", "Amount_Drug", "Method_Drug", "ADE_Drug"],
        "B2": ["Drug_Disease", "Treatment_Disease"],
        "B3": ["Amount_Drug", "ADE_Drug", "Method_Drug"],
        "B4": ["ADE_Drug"],
        "B5": ["Treatment_Disease", "Operation_Disese"],
        "B6": ["Operation_Disease", "Drug_Disease", "Treatment_Disease", "Test_Items_Disease"],
        "C0": ["Symptom_Disease", "Drug_Disease"],
        "C1": ["Class_Disease"],
        "C2": ["Reason_Disease", "Pathogenesis_Disease"],
        "C3": ["Anatomy_Disease", "Reason_Disease", "Pathogenesis_Disease"],
        "C4": ["Pathogenesis_Disease", "Reason_Disease"],
        "D0": ["Pathogenesis_Disease", "Reason_Disease"],
        "D1": ["Pathogenesis_Disease", "Reason_Disease"],
        "D2": ["Pathogenesis_Disease", "Reason_Disease"],
        "D3": ["Pathogenesis_Disease", "Reason_Disease"],
        "E1": ["Pathogenesis_Disease", "Reason_Disease", "Symptom_Disease"],
        "E2": ["Pathogenesis_Disease", "Reason_Disease", "Symptom_Disease"],
        "E3": ["Pathogenesis_Disease", "Reason_Disease", "Symptom_Disease"],
    }
    relations = disease_relations.get(intent[:2], ["Symptom_Disease"])
    KG_results = neo4j_query(graph, entities, relations)

    # 4. RAG 检索
    top_results = retrieve_answer(query, knowledge_base, embedding_model, top_k=3)
    rag_res = [result[1]["content"] for result in top_results]

    # 5. 构造提示，并生成最终回答（调用大模型）
    custom_prompt = (
        f"病人提出的问题是：'{query}'。\n"
        f"【实体识别】：{', '.join(entities) if entities else '无'}\n"
        f"【意图】：{intent}\n"
        f"【知识图谱查询结果】：{KG_results if KG_results else '无'}\n"
        f"【文档检索结果】：{rag_res if rag_res else '无'}\n\n"
        #f"请以资深糖尿病专家的身份，给出一个详细、专业且易于理解的回答。"
        f"请以资深糖尿病专家的身份，给出一个200个字左右的回答。"
    )

    # 采用自定义链调用大模型
    from langchain_core.prompts import ChatPromptTemplate
    from langchain_openai import ChatOpenAI
    from langchain_core.output_parsers import StrOutputParser

    custom_prompt_template = ChatPromptTemplate.from_messages([
        ("system", "你现在扮演一名资深糖尿病专家。"),
        ("user", custom_prompt)
    ])

    custom_chain = (
        custom_prompt_template
        | ChatOpenAI(
            temperature=0.95,
            model="glm-4-flash",
            openai_api_key=os.getenv("ZHIPUAI_API_KEY"),
            base_url=os.getenv("ZHIPUAI_BASE_URL")
        )
        | StrOutputParser()
    )
    final_answer = custom_chain.invoke({"name": "糖尿病专家", "text": custom_prompt})
    return final_answer

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
