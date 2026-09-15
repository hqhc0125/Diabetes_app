package com.example.bysjapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {
    private RecyclerView favoriteRecyclerView;
    private FavoriteAdapter favoriteAdapter;
    private FavoriteManager favoriteManager;
    private List<EducationItem> favoriteList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        favoriteManager = new FavoriteManager(this);
        favoriteList = favoriteManager.getFavoriteList();

        favoriteRecyclerView = findViewById(R.id.favorite_recycler_view);
        favoriteRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        favoriteAdapter = new FavoriteAdapter(favoriteList, this, favoriteManager);
        favoriteRecyclerView.setAdapter(favoriteAdapter);
    }
}