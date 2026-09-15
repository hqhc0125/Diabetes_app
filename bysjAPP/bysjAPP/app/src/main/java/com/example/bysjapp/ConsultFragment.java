//package com.example.bysjapp;
//
//import android.content.Intent;
//import android.database.sqlite.SQLiteDatabase;
//import android.os.Bundle;
//import android.text.TextUtils;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.fragment.app.Fragment;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import org.neo4j.driver.AuthTokens;
//import org.neo4j.driver.Driver;
//import org.neo4j.driver.GraphDatabase;
//import org.neo4j.driver.Record;
//import org.neo4j.driver.Session;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import okhttp3.Call;
//import okhttp3.Callback;
//import okhttp3.MediaType;
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.RequestBody;
//import okhttp3.Response;
//
//import com.google.gson.Gson;
//import com.google.gson.annotations.SerializedName;
//
//public class ConsultFragment extends Fragment {
//
//    // UI 控件
//    private RecyclerView rvChat;
//    private EditText etMessage;
//    private Button btnSend;
//    private Button btnConsult, btnConsultHistory;
//    private List<ConsultFragment.ChatMessage> chatMessages = new ArrayList<>();
//    private ChatAdapter chatAdapter;
//
//    // 智谱 API 相关常量，请替换 YOUR_API_KEY 为你实际的 API 密钥
//    private static final String API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
//    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
//    private OkHttpClient client = new OkHttpClient();
//    private Gson gson = new Gson();
//
//    // Neo4j 图数据库连接（请将 URI 修改为正确的地址，通常使用 bolt 协议）
//    private Driver neo4jDriver;
//    //private static final String NEO4J_URI = "bolt://localhost:7687"; // 修改为正确地址
//    private static final String NEO4J_URI = "bolt://10.0.2.2:7687";
//
//    private static final String NEO4J_USERNAME = "neo4j";
//    private ConsultHistoryDatabaseHelper dbHelper;
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        // 初始化 Neo4j 连接
//        neo4jDriver = GraphDatabase.driver(NEO4J_URI, AuthTokens.basic(NEO4J_USERNAME, NEO4J_PASSWORD));
//
//        try (Session session = neo4jDriver.session()) {
//            session.run("RETURN 1").consume(); // 测试简单查询
//            Log.d("Neo4j", "成功连接到 Neo4j 数据库");
//        } catch (Exception e) {
//            Log.e("Neo4j", "Neo4j 连接失败: " + e.getMessage());
//        }
//
//    }
//
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        // 关闭 Neo4j 连接
//        if (neo4jDriver != null) {
//            neo4jDriver.close();
//        }
//    }
//
//    // 数据模型：代表一条聊天消息
//    public static class ChatMessage {
//        public static final int TYPE_USER = 0;
//        public static final int TYPE_BOT = 1;
//        private String message;
//        private int type;  // 0: 用户, 1: Bot
//
//        public ChatMessage(String message, int type) {
//            this.message = message;
//            this.type = type;
//        }
//        public String getMessage() { return message; }
//        public int getType() { return type; }
//    }
//
//    // 请求数据模型：生成符合智谱 API 要求的 JSON格式
//    public static class ZhipuRequest {
//        @SerializedName("model")
//        private String model;
//        @SerializedName("messages")
//        private List<Message> messages;
//
//        public ZhipuRequest(String question) {
//            this.model = "glm-4-flash";
//            this.messages = new ArrayList<>();
//            // 当前仅发送用户问题
//            this.messages.add(new Message("user", question));
//        }
//
//        public static class Message {
//            @SerializedName("role")
//            private String role;
//            @SerializedName("content")
//            private String content;
//            public Message(String role, String content) {
//                this.role = role;
//                this.content = content;
//            }
//        }
//    }
//
//    // 响应数据模型：解析智谱 API 返回数据，根据示例取 choices[0].message.content
//    public static class ZhipuResponse {
//        @SerializedName("choices")
//        private Choice[] choices;
//        public Choice[] getChoices() { return choices; }
//
//        public static class Choice {
//            @SerializedName("finish_reason")
//            private String finishReason;
//            @SerializedName("index")
//            private int index;
//            @SerializedName("message")
//            private Message message;
//            public Message getMessage() { return message; }
//        }
//
//        public static class Message {
//            @SerializedName("content")
//            private String content;
//            @SerializedName("role")
//            private String role;
//            public String getContent() { return content; }
//        }
//    }
//
//    private String queryKnowledgeGraph(String userQuery) {
//        try (Session session = neo4jDriver.session()) {
//            // 提取关键词
//            String extractedKeyword = extractKeyword(userQuery);
//            if (extractedKeyword == null || extractedKeyword.isEmpty()) {
//                return "未能从您的问题中识别关键字，请尝试更具体的描述。";
//            }
//
//            // 动态构建 Cypher 查询
//            String cypherQuery =
//                    "MATCH (entity) " +
//                            "WHERE entity.name CONTAINS $query " +
//                            "OPTIONAL MATCH (entity)-[relation]->(connectedEntity) " +
//                            "RETURN entity.name AS EntityName, " +
//                            "       labels(entity) AS EntityType, " +
//                            "       collect(relation.relation_type) AS Relations, " +
//                            "       collect(connectedEntity.name) AS RelatedEntities LIMIT 10";
//
//            Map<String, Object> parameters = new HashMap<>();
//            parameters.put("query", extractedKeyword);
//
//            List<String> results = new ArrayList<>();
//            session.run(cypherQuery, parameters).list().forEach(record -> {
//                String entityName = record.get("EntityName").asString();
//                List<String> entityTypes = record.get("EntityType").asList(obj -> obj.toString());
//                List<String> relations = record.get("Relations").asList(obj -> obj.toString());
//                List<String> relatedEntities = record.get("RelatedEntities").asList(obj -> obj.toString());
//
//                // 格式化结果
//                results.add("实体: " + entityName +
//                        "\n类型: " + String.join(", ", entityTypes) +
//                        "\n关系: " + (relations.isEmpty() ? "无" : String.join(", ", relations)) +
//                        "\n关联实体: " + (relatedEntities.isEmpty() ? "无" : String.join(", ", relatedEntities)));
//            });
//
//            if (results.isEmpty()) {
//                return "知识图谱中未找到相关信息。";
//            }
//            return String.join("\n\n", results);
//        } catch (Exception e) {
//            return "查询时发生错误：" + e.getMessage();
//        }
//    }
//
//
//
//    private String extractKeyword(String userQuery) {
//        List<String> predefinedKeywords = Arrays.asList(
//                "糖尿病", "胰岛素", "T2DM", "低血糖", "体重增加", "空腹血糖", "HbA1c", "磺脲类药物", "治疗", "分类", "心血管"
//        );
//        for (String keyword : predefinedKeywords) {
//            if (userQuery.contains(keyword)) {
//                return keyword;
//            }
//        }
//        return null; // 未匹配到时返回 null
//    }
//
//
//
//
//
//
//    // RecyclerView Adapter
//    private class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
//        private List<ConsultFragment.ChatMessage> messages;
//        private static final int VIEW_TYPE_USER = 0;
//        private static final int VIEW_TYPE_BOT = 1;
//
//        public ChatAdapter(List<ConsultFragment.ChatMessage> messages) {
//            this.messages = messages;
//        }
//
//        @Override
//        public int getItemViewType(int position) {
//            return messages.get(position).getType();
//        }
//
//        @NonNull
//        @Override
//        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//            if (viewType == VIEW_TYPE_USER) {
//                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_user, parent, false);
//                return new UserViewHolder(view);
//            } else {  // VIEW_TYPE_BOT
//                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_bot, parent, false);
//                return new BotViewHolder(view);
//            }
//        }
//
//        @Override
//        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
//            ChatMessage message = messages.get(position);
//            if (holder instanceof UserViewHolder) {
//                ((UserViewHolder) holder).tvMessage.setText(message.getMessage());
//            } else if (holder instanceof BotViewHolder) {
//                ((BotViewHolder) holder).tvMessage.setText(message.getMessage());
//            }
//        }
//
//        @Override
//        public int getItemCount() {
//            return messages.size();
//        }
//
//        class UserViewHolder extends RecyclerView.ViewHolder {
//            TextView tvMessage;
//            public UserViewHolder(@NonNull View itemView) {
//                super(itemView);
//                tvMessage = itemView.findViewById(R.id.tv_user_message);
//            }
//        }
//
//        class BotViewHolder extends RecyclerView.ViewHolder {
//            TextView tvMessage;
//            public BotViewHolder(@NonNull View itemView) {
//                super(itemView);
//                tvMessage = itemView.findViewById(R.id.tv_bot_message);
//            }
//        }
//    }
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        // 加载布局文件 fragment_consult.xml
//        View view = inflater.inflate(R.layout.fragment_consult, container, false);
//        dbHelper = new ConsultHistoryDatabaseHelper(getContext());
//
//        // 初始化顶部切换按钮
//        btnConsult = view.findViewById(R.id.btn_consult);
//        btnConsultHistory = view.findViewById(R.id.btn_consult_history);
//        btnConsult.setOnClickListener(v -> {
//            btnConsult.setBackgroundColor(getResources().getColor(R.color.selectedColor));
//            btnConsult.setTextColor(getResources().getColor(android.R.color.white));
//            btnConsultHistory.setBackgroundColor(getResources().getColor(R.color.unselectedColor));
//            btnConsultHistory.setTextColor(getResources().getColor(android.R.color.black));
//        });
//        btnConsultHistory.setOnClickListener(v -> {
//            btnConsultHistory.setBackgroundColor(getResources().getColor(R.color.selectedColor));
//            btnConsultHistory.setTextColor(getResources().getColor(android.R.color.white));
//            btnConsult.setBackgroundColor(getResources().getColor(R.color.unselectedColor));
//            btnConsult.setTextColor(getResources().getColor(android.R.color.black));
//            Intent intent = new Intent(getActivity(), ConsultHistoryActivity.class);
//            startActivity(intent);
//        });
//
//        // 初始化 RecyclerView 和输入区域
//        rvChat = view.findViewById(R.id.rv_chat);
//        etMessage = view.findViewById(R.id.et_message);
//        btnSend = view.findViewById(R.id.btn_send);
//        rvChat.setLayoutManager(new LinearLayoutManager(getContext()));
//        chatAdapter = new ChatAdapter(chatMessages);
//        rvChat.setAdapter(chatAdapter);
//
//        btnSend.setOnClickListener(v -> {
//            String userMsg = etMessage.getText().toString().trim();
//            if (TextUtils.isEmpty(userMsg)) {
//                Toast.makeText(getContext(), "请输入消息", Toast.LENGTH_SHORT).show();
//                return;
//            }
//            // 添加用户消息
//            addMessage(new ChatMessage(userMsg, ChatMessage.TYPE_USER));
//            etMessage.setText("");
//            // 调用智谱 API 获取 Bot 回复，并结合 Neo4j 知识图谱增强回答
//            fetchBotResponse(userMsg);
//        });
//
//        return view;
//    }
//
//    private void addMessage(ChatMessage message) {
//        chatMessages.add(message);
//        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
//        rvChat.smoothScrollToPosition(chatMessages.size() - 1);
//    }
//
//    private void saveChatHistory(String title, String content) {
//        SQLiteDatabase db = dbHelper.getWritableDatabase();
//        String timestamp = String.valueOf(System.currentTimeMillis());
//        String insertSQL = "INSERT INTO ConsultHistory (title, timestamp, content) " +
//                "VALUES ('" + title + "', '" + timestamp + "', '" + content + "')";
//        db.execSQL(insertSQL);
//        db.close();
//    }
//
//    private List<Map<String, String>> conversationHistory = new ArrayList<>();
//
//    private void fetchBotResponse(String userQuestion) {
//        // 添加用户输入到上下文记录（线程安全）
//        conversationHistory.add(Map.of("role", "user", "content", userQuestion));
//
//        // 构造请求体
//        String jsonRequestBody = gson.toJson(Map.of(
//                "model", "glm-4-flash",
//                "messages", new ArrayList<>(conversationHistory) // 使用副本避免并发问题
//        ));
//        Log.d("ConsultFragment", "Request JSON: " + jsonRequestBody);
//
//        RequestBody requestBody = RequestBody.create(jsonRequestBody, JSON);
//        Request request = new Request.Builder()
//                .url(API_URL)
//                .addHeader("Authorization", "Bearer " + API_KEY)
//                .post(requestBody)
//                .build();
//
//        client.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(@NonNull Call call, @NonNull IOException e) {
//                Log.e("ConsultFragment", "API 请求失败：" + e.getMessage());
//                if (getActivity() != null) {
//                    getActivity().runOnUiThread(() -> addMessage(new ChatMessage("Bot 未能获取响应，请检查网络连接。", ChatMessage.TYPE_BOT)));
//                }
//            }
//
//            @Override
//            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                if (!response.isSuccessful() || response.body() == null) {
//                    Log.e("ConsultFragment", "API 响应失败，状态码：" + response.code());
//                    if (getActivity() != null) {
//                        getActivity().runOnUiThread(() -> addMessage(new ChatMessage("Bot 响应失败，请稍后再试。", ChatMessage.TYPE_BOT)));
//                    }
//                    return;
//                }
//
//                // 解析响应体
//                String responseBody = response.body().string();
//                Log.d("ConsultFragment", "API Response: " + responseBody);
//                ZhipuResponse zhipuResponse = gson.fromJson(responseBody, ZhipuResponse.class);
//
//                String botAnswer = "";
//                if (zhipuResponse != null && zhipuResponse.getChoices() != null && zhipuResponse.getChoices().length > 0 &&
//                        zhipuResponse.getChoices()[0].getMessage() != null) {
//                    botAnswer = zhipuResponse.getChoices()[0].getMessage().getContent();
//                } else {
//                    botAnswer = "Bot 未能提供有效的回答，请稍后再试。";
//                    Log.e("ConsultFragment", "API 响应内容不完整。");
//                }
//
//                // 添加机器人回复到上下文（线程安全）
//                String finalBotAnswer = botAnswer;
//                getActivity().runOnUiThread(() -> {
//                    conversationHistory.add(Map.of("role", "assistant", "content", finalBotAnswer));
//                    String enhancedAnswer = finalBotAnswer + "\n\n知识图谱检索结果：\n" + queryKnowledgeGraph(userQuestion);
//                    addMessage(new ChatMessage(enhancedAnswer, ChatMessage.TYPE_BOT));
//                });
//
//                // 保存对话记录
//                String finalDialog = "";
//                for (ChatMessage message : chatMessages) {
//                    finalDialog += (message.getType() == ChatMessage.TYPE_USER ? "用户: " : "Bot: ") + message.getMessage() + "\n";
//                }
//                String title = chatMessages.size() > 0 ? chatMessages.get(0).getMessage() : "对话记录";
//                saveChatHistory(title, finalDialog);
//            }
//        });
//    }
//
//
//}


package com.example.bysjapp;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ConsultFragment extends Fragment {
    private static final String TAG = "ConsultFragment";
    private static final String BASE_URL = "http://10.0.2.2:5000";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    // 顶部固定区域控件（标题、历史按钮、时间戳）
    private TextView tvTitle, tvTimestamp;
    private Button btnHistory;
    // 聊天区域和输入区域控件
    private RecyclerView rvChat;
    private EditText etQuestion;
    private Button btnSend;

    // 聊天列表，由于需要混合图片（头部）和聊天消息，因此使用 Object 集合
    private List<Object> chatItems = new ArrayList<>();
    private ChatAdapter chatAdapter;

    private OkHttpClient client;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_consult, container, false);

        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        // 初始化控件
        initViews(view);
        // 设置“历史”按钮点击事件
        setupHistoryButton();
        setupChatRecyclerView();
        setupSendButton();
        updateTimestamp();

        // 将医生漫画图片作为对话头部项添加到列表中
        chatItems.add(R.drawable.doctor_cartoon);
        chatAdapter.notifyItemInserted(0);

        return view;
    }

    private void initViews(View view) {
        tvTitle = view.findViewById(R.id.tv_title);
        btnHistory = view.findViewById(R.id.btn_history);
        tvTimestamp = view.findViewById(R.id.tv_timestamp);
        rvChat = view.findViewById(R.id.rv_chat);
        etQuestion = view.findViewById(R.id.et_question);
        btnSend = view.findViewById(R.id.btn_send);
    }

    private void setupHistoryButton() {
        if (btnHistory == null) return;
        btnHistory.setOnClickListener(v -> {
            if (getActivity() != null) {
                // 跳转到HistoryActivity页面
                Intent intent = new Intent(getActivity(), ConsultHistoryActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setupChatRecyclerView() {
        chatAdapter = new ChatAdapter(chatItems);
        rvChat.setLayoutManager(new LinearLayoutManager(getContext()));
        rvChat.setAdapter(chatAdapter);
    }

    private void setupSendButton() {
        btnSend.setOnClickListener(v -> {
            String question = etQuestion.getText().toString().trim();
            if (TextUtils.isEmpty(question)) {
                showToast("请输入问题");
                return;
            }
            // 添加用户消息到聊天列表
            addMessage(new ChatMessage(question, true));
            etQuestion.setText("");
            // 调用后端接口获取机器人回答
            fetchAnswerFromServer(question);
        });
    }

    private void addMessage(ChatMessage message) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            chatItems.add(message);
            chatAdapter.notifyItemInserted(chatItems.size() - 1);
            rvChat.smoothScrollToPosition(chatItems.size() - 1);
        });
    }

    private void saveConsultHistory(String question, String answer) {
        ConsultHistoryDatabaseHelper dbHelper = new ConsultHistoryDatabaseHelper(getActivity());
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("title", "咨询记录");
        values.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        values.put("content", "用户：" + question + "\n机器人：" + answer);

        db.insert("ConsultHistory", null, values);
        db.close();
    }

    private void fetchAnswerFromServer(String question) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("question", question);

            RequestBody body = RequestBody.create(jsonBody.toString(), JSON_MEDIA_TYPE);
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/chat")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "API请求失败: " + e.getMessage());
                    showToast("网络请求失败: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        showToast("服务器响应错误: " + response.code());
                        return;
                    }
                    try {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        JSONObject json = new JSONObject(responseBody);
                        if (json.has("success") && json.getBoolean("success")) {
                            String answer = json.getString("final_answer");
                            getActivity().runOnUiThread(() -> {
                                addMessage(new ChatMessage(answer, false));
                                saveConsultHistory(question, answer); // 保存咨询记录
                            });
                        } else {
                            showToast("无效的响应格式");
                        }
                    } catch (JSONException e) {
                        showToast("数据解析失败");
                    }
                }
            });
        } catch (JSONException e) {
            showToast("请求构建失败");
        }
    }

    private void showToast(String message) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() ->
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
    }

    // 更新时间戳（格式：yyyy-MM-dd HH:mm:ss）
    private void updateTimestamp() {
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        if (tvTimestamp != null) {
            tvTimestamp.setText(currentTime);
        }
    }

    // 聊天消息数据类
    public static class ChatMessage {
        private String content;
        private boolean isUser;

        public ChatMessage(String content, boolean isUser) {
            this.content = content;
            this.isUser = isUser;
        }

        public String getContent() {
            return content;
        }

        public boolean isUser() {
            return isUser;
        }
    }

    // 内部适配器：支持两种项——图片项和聊天项（用户/机器人）
    private static class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_IMAGE = 0;
        private static final int TYPE_USER = 1;
        private static final int TYPE_BOT = 2;
        private final List<Object> items;

        public ChatAdapter(List<Object> items) {
            this.items = items;
        }

        @Override
        public int getItemViewType(int position) {
            Object item = items.get(position);
            if (item instanceof Integer) {
                return TYPE_IMAGE;
            } else {
                ChatMessage msg = (ChatMessage) item;
                return msg.isUser() ? TYPE_USER : TYPE_BOT;
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_IMAGE) {
                View view = inflater.inflate(R.layout.item_image_doctor, parent, false);
                return new ImageViewHolder(view);
            } else if (viewType == TYPE_USER) {
                View view = inflater.inflate(R.layout.item_chat_user, parent, false);
                return new UserViewHolder(view);
            } else {  // TYPE_BOT
                View view = inflater.inflate(R.layout.item_chat_bot, parent, false);
                return new BotViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            int viewType = getItemViewType(position);
            if (viewType == TYPE_IMAGE) {
                ((ImageViewHolder) holder).bind((int) items.get(position));
            } else if (viewType == TYPE_USER) {
                ((UserViewHolder) holder).bind((ChatMessage) items.get(position));
            } else {
                ((BotViewHolder) holder).bind((ChatMessage) items.get(position));
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ImageViewHolder extends RecyclerView.ViewHolder {
            private final android.widget.ImageView imageView;
            public ImageViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.item_image_doctor);
            }
            public void bind(int resId) {
                imageView.setImageResource(resId);
            }
        }

        static class UserViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvMessage;
            public UserViewHolder(@NonNull View itemView) {
                super(itemView);
                tvMessage = itemView.findViewById(R.id.tv_user_message);
                if (tvMessage == null) {
                    throw new IllegalStateException("找不到TextView，请检查布局文件 item_chat_user.xml");
                }
            }
            public void bind(ChatMessage message) {
                tvMessage.setText(message.getContent());
            }
        }

        static class BotViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvMessage;
            public BotViewHolder(@NonNull View itemView) {
                super(itemView);
                tvMessage = itemView.findViewById(R.id.tv_bot_message);
                if (tvMessage == null) {
                    throw new IllegalStateException("找不到TextView，请检查布局文件 item_chat_bot.xml");
                }
            }
            public void bind(ChatMessage message) {
                tvMessage.setText(message.getContent());
            }
        }
    }
}




