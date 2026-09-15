package com.example.bysjapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder> {
    private List<EducationItem> favoriteList;
    private Context context;
    private FavoriteManager favoriteManager;

    public FavoriteAdapter(List<EducationItem> favoriteList, Context context, FavoriteManager favoriteManager) {
        this.favoriteList = favoriteList;
        this.context = context;
        this.favoriteManager = favoriteManager;
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_favorite, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        EducationItem item = favoriteList.get(position);
        holder.favoriteTitle.setText(item.getTitle());
        holder.favoriteDescription.setText(item.getDescription());
        holder.favoriteImage.setImageResource(item.getImageResId());

        holder.favoriteIcon.setOnClickListener(v -> {
            favoriteManager.removeFromFavorites(item);
            favoriteList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, favoriteList.size());
            item.setFavorite(false);
            Toast.makeText(context, "取消收藏", Toast.LENGTH_SHORT).show();
        });

        holder.itemView.setOnClickListener(v -> {
            // 处理点击事件，跳转到文章详情页
            Intent intent = new Intent(context, WebViewActivity.class);
            intent.putExtra("url", item.getUrl());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return favoriteList.size();
    }

    public static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        TextView favoriteTitle;
        TextView favoriteDescription;
        ImageView favoriteImage;
        ImageView favoriteIcon;

        public FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            favoriteTitle = itemView.findViewById(R.id.tv_favorite_title);
            favoriteDescription = itemView.findViewById(R.id.tv_favorite_description);
            favoriteImage = itemView.findViewById(R.id.iv_favorite_image);
            favoriteIcon = itemView.findViewById(R.id.iv_favorite_remove);
        }
    }
}