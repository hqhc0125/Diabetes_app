//package com.example.bysjapp;
//
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//import java.util.List;
//
//// ChatAdapter.java
//public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
//    private List<ChatMessage> messages;
//
//    public static class ViewHolder extends RecyclerView.ViewHolder {
//        TextView tvContent;
//        public ViewHolder(View itemView) {
//            super(itemView);
//            tvContent = itemView.findViewById(R.id.tv_user_message);
//        }
//    }
//
//    public ChatAdapter(List<ChatMessage> messages) {
//        this.messages = messages;
//    }
//
//    @Override
//    public int getItemViewType(int position) {
//        return messages.get(position).isUser() ? 1 : 0;
//    }
//
//    @NonNull
//    @Override
//    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext()).inflate(
//                viewType == 1 ? R.layout.item_chat_user : R.layout.item_chat_bot,
//                parent, false);
//        return new ViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(ViewHolder holder, int position) {
//        holder.tvContent.setText(messages.get(position).getContent());
//    }
//
//    @Override
//    public int getItemCount() {
//        return messages.size();
//    }
//}