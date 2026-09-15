package com.example.bysjapp;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommunityFragment extends Fragment {

    private RecyclerView rvComments;
    private EditText etComment;
    private Button btnSend;
    private CommentAdapter commentAdapter;
    private List<Comment> commentList = new ArrayList<>();
    private CommentDatabaseHelper commentDatabaseHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_community, container, false);

        rvComments = view.findViewById(R.id.rv_comments);
        etComment = view.findViewById(R.id.et_comment);
        btnSend = view.findViewById(R.id.btn_send);

        commentDatabaseHelper = new CommentDatabaseHelper(requireContext());
        loadCommentsFromDatabase();

        commentAdapter = new CommentAdapter(commentList);
        rvComments.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvComments.setAdapter(commentAdapter);

        btnSend.setOnClickListener(v -> {
            String commentContent = etComment.getText().toString().trim();
            if (!commentContent.isEmpty()) {
                String username = "用户79078465"; // 可替换为实际用户名
                String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                Comment comment = new Comment(username, time, commentContent);
                commentList.add(comment);
                commentAdapter.notifyDataSetChanged();
                saveCommentToDatabase(comment);
                etComment.setText("");
            } else {
                Toast.makeText(requireContext(), "请输入评论内容", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void loadCommentsFromDatabase() {
        SQLiteDatabase db = commentDatabaseHelper.getReadableDatabase();
        Cursor cursor = db.query(CommentDatabaseHelper.getTableName(), null, null, null, null, null, null);
        while (cursor.moveToNext()) {
            @SuppressLint("Range") String username = cursor.getString(cursor.getColumnIndex(CommentDatabaseHelper.getColumnUsername()));
            @SuppressLint("Range") String time = cursor.getString(cursor.getColumnIndex(CommentDatabaseHelper.getColumnTime()));
            @SuppressLint("Range") String content = cursor.getString(cursor.getColumnIndex(CommentDatabaseHelper.getColumnContent()));
            Comment comment = new Comment(username, time, content);
            commentList.add(comment);
        }
        cursor.close();
        db.close();
    }

    private void saveCommentToDatabase(Comment comment) {
        SQLiteDatabase db = commentDatabaseHelper.getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(CommentDatabaseHelper.getColumnUsername(), comment.getUsername());
        values.put(CommentDatabaseHelper.getColumnTime(), comment.getTime());
        values.put(CommentDatabaseHelper.getColumnContent(), comment.getContent());
        db.insert(CommentDatabaseHelper.getTableName(), null, values);
        db.close();
    }

    // 模型类：代表一条评论
    public static class Comment {
        private String username;
        private String time;
        private String content;
        private boolean isLiked; // 添加点赞状态字段
        private List<Comment> replies; // 存储子评论的列表

        public Comment(String username, String time, String content) {
            this.username = username;
            this.time = time;
            this.content = content;
            this.isLiked = false; // 默认未点赞
            this.replies = new ArrayList<>(); // 初始化子评论列表
        }

        public String getUsername() {
            return username;
        }

        public String getTime() {
            return time;
        }

        public String getContent() {
            return content;
        }

        public boolean isLiked() {
            return isLiked;
        }

        public void setLiked(boolean liked) {
            isLiked = liked;
        }

        public List<Comment> getReplies() {
            return replies;
        }

        public void addReply(Comment reply) {
            replies.add(reply);
        }
    }

    // RecyclerView Adapter
    private class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

        private List<Comment> comments;

        public CommentAdapter(List<Comment> comments) {
            this.comments = comments;
        }

        @NonNull
        @Override
        public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
            return new CommentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
            Comment comment = comments.get(position);
            holder.tvUsername.setText(comment.getUsername());
            holder.tvTime.setText(comment.getTime());
            holder.tvContent.setText(comment.getContent());

            // 根据点赞状态设置图标
            if (comment.isLiked()) {
                holder.btnLike.setImageResource(R.drawable.liked_icon); // 替换为点赞后的图标
            } else {
                holder.btnLike.setImageResource(R.drawable.unliked_icon); // 替换为未点赞的图标
            }

            holder.btnLike.setOnClickListener(v -> {
                // 切换点赞状态
                comment.setLiked(!comment.isLiked());
                // 根据新的点赞状态设置图标
                if (comment.isLiked()) {
                    holder.btnLike.setImageResource(R.drawable.liked_icon); // 替换为点赞后的图标
                    Toast.makeText(requireContext(), "点赞成功", Toast.LENGTH_SHORT).show();
                } else {
                    holder.btnLike.setImageResource(R.drawable.unliked_icon); // 替换为未点赞的图标
                    Toast.makeText(requireContext(), "取消点赞", Toast.LENGTH_SHORT).show();
                }
            });

            holder.btnReply.setOnClickListener(v -> {
                // 显示回复输入框
                holder.replyLayout.setVisibility(View.VISIBLE);
            });

            holder.btnSendReply.setOnClickListener(v -> {
                String replyContent = holder.etReply.getText().toString().trim();
                if (!replyContent.isEmpty()) {
                    String username = "用户79078465"; // 可替换为实际用户名
                    String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                    // 创建回复评论对象
                    Comment replyComment = new Comment(username, time, replyContent);
                    // 将回复评论添加到父评论的子评论列表中
                    comment.addReply(replyComment);
                    // 保存回复评论到数据库
                    saveCommentToDatabase(replyComment);
                    // 隐藏回复输入框
                    holder.replyLayout.setVisibility(View.GONE);
                    holder.etReply.setText("");
                    // 刷新子评论列表
                    holder.repliesAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(requireContext(), "请输入回复内容", Toast.LENGTH_SHORT).show();
                }
            });

            // 设置子评论列表的适配器
            holder.repliesAdapter = new CommentAdapter(comment.getReplies());
            holder.rvReplies.setLayoutManager(new LinearLayoutManager(requireContext()));
            holder.rvReplies.setAdapter(holder.repliesAdapter);
        }

        @Override
        public int getItemCount() {
            return comments.size();
        }

        public class CommentViewHolder extends RecyclerView.ViewHolder {

            TextView tvUsername;
            TextView tvTime;
            TextView tvContent;
            ImageButton btnLike;
            ImageButton btnReply;
            LinearLayout replyLayout;
            EditText etReply;
            Button btnSendReply;
            RecyclerView rvReplies;
            CommentAdapter repliesAdapter;

            public CommentViewHolder(@NonNull View itemView) {
                super(itemView);
                tvUsername = itemView.findViewById(R.id.tv_username);
                tvTime = itemView.findViewById(R.id.tv_time);
                tvContent = itemView.findViewById(R.id.tv_content);
                btnLike = itemView.findViewById(R.id.btn_like);
                btnReply = itemView.findViewById(R.id.btn_reply);
                replyLayout = itemView.findViewById(R.id.reply_layout);
                etReply = itemView.findViewById(R.id.et_reply);
                btnSendReply = itemView.findViewById(R.id.btn_send_reply);
                rvReplies = itemView.findViewById(R.id.rv_replies);
            }
        }
    }
}