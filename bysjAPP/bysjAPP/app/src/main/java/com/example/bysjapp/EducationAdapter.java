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
import java.util.ArrayList;
import java.util.List;

public class EducationAdapter extends RecyclerView.Adapter<EducationAdapter.EducationViewHolder> {
    private List<EducationItem> displayList;
    private List<EducationItem> fullList;
    private Context context;
    private FavoriteManager favoriteManager;
    private boolean isExpanded = false;

    public EducationAdapter(List<EducationItem> educationList, Context context, FavoriteManager favoriteManager) {
        this.displayList = new ArrayList<>(educationList);
        this.fullList = new ArrayList<>(educationList);
        this.context = context;
        this.favoriteManager = favoriteManager;
        updateDisplayList();
    }

    private void updateDisplayList() {
        if (!isExpanded && fullList.size() > 3) {
            displayList = new ArrayList<>(fullList.subList(0, 3));
        } else {
            displayList = new ArrayList<>(fullList);
        }
    }

    // 展开或折叠内容
    public void setExpanded(boolean expanded) {
        this.isExpanded = expanded;
        updateDisplayList();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EducationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.education_item, parent, false);
        return new EducationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EducationViewHolder holder, int position) {
        EducationItem item = displayList.get(position);
        holder.title.setText(item.getTitle());
        holder.description.setText(item.getDescription());
        holder.image.setImageResource(item.getImageResId());

        boolean isFavorite = item.isFavorite();
        holder.favoriteIcon.setImageResource(isFavorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);

        holder.favoriteIcon.setOnClickListener(v -> {
            if (isFavorite) {
                favoriteManager.removeFromFavorites(item);
                holder.favoriteIcon.setImageResource(R.drawable.ic_favorite_border);
                item.setFavorite(false);
                Toast.makeText(context, "取消收藏", Toast.LENGTH_SHORT).show();
            } else {
                favoriteManager.addToFavorites(item);
                holder.favoriteIcon.setImageResource(R.drawable.ic_favorite);
                item.setFavorite(true);
                Toast.makeText(context, "已收藏", Toast.LENGTH_SHORT).show();
            }
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, WebViewActivity.class);
            intent.putExtra("url", item.getUrl());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    public void filter(String query) {
        List<EducationItem> filtered = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            filtered.addAll(fullList);
        } else {
            for (EducationItem item : fullList) {
                if (item.getTitle().toLowerCase().contains(query.toLowerCase())) {
                    filtered.add(item);
                }
            }
        }
        displayList = filtered;
        notifyDataSetChanged();
    }

    public static class EducationViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView description;
        ImageView image;
        ImageView favoriteIcon;

        public EducationViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.education_title);
            description = itemView.findViewById(R.id.education_description);
            image = itemView.findViewById(R.id.education_image);
            favoriteIcon = itemView.findViewById(R.id.favorite_icon);
        }
    }
}