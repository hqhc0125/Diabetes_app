from zhipuai import ZhipuAI
import os
from dotenv import load_dotenv

load_dotenv(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".env"))


def ask_glm(content):
    client = ZhipuAI(api_key=os.getenv("ZHIPUAI_API_KEY"))

    # 调用ZhipuAI的chat.completions接口，传入参数
    response = client.chat.completions.create(
        model="glm-4-flash",  # 指定模型名称
        messages=[
            {"role": "user", "content": content}  # 使用传入的content作为对话的用户输入
        ],
    )

    # 返回响应中的生成结果
    return response.choices[0].message

if __name__ == '__main__':
    prompt = '''我血糖过高怎么办
        '''
    #     answer = ask_glm(prompt)['choices'][0]['message']['content']
    answer = ask_glm(prompt).content
    print(answer)

