package com.example.bysjapp;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FavoriteManager {
    private static final String PREF_NAME = "FavoritePrefs";
    private static final String KEY_FAVORITES = "favorites";
    private SharedPreferences sharedPreferences;
    private Gson gson;

    public FavoriteManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public List<EducationItem> getFavoriteList() {
        String json = sharedPreferences.getString(KEY_FAVORITES, "");
        Type type = new TypeToken<ArrayList<EducationItem>>() {}.getType();
        List<EducationItem> favoriteList = gson.fromJson(json, type);
        return favoriteList != null ? favoriteList : new ArrayList<>();
    }

    public void addToFavorites(EducationItem item) {
        List<EducationItem> favoriteList = getFavoriteList();
        if (!favoriteList.contains(item)) {
            favoriteList.add(item);
            saveFavoriteList(favoriteList);
        }
    }

    public void removeFromFavorites(EducationItem item) {
        List<EducationItem> favoriteList = getFavoriteList();
        if (favoriteList.contains(item)) {
            favoriteList.remove(item);
            saveFavoriteList(favoriteList);
        }
    }

    private void saveFavoriteList(List<EducationItem> favoriteList) {
        String json = gson.toJson(favoriteList);
        sharedPreferences.edit().putString(KEY_FAVORITES, json).apply();
    }
}