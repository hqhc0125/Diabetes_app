package com.example.bysjapp;

import java.util.Objects;

public class EducationItem {
    private String title;
    private String description;
    private int imageResId;
    private String url;
    private boolean isFavorite;

    public EducationItem(String title, String description, int imageResId, String url) {
        this.title = title;
        this.description = description;
        this.imageResId = imageResId;
        this.url = url;
        this.isFavorite = false;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getImageResId() {
        return imageResId;
    }

    public String getUrl() {
        return url;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EducationItem that = (EducationItem) o;
        return imageResId == that.imageResId &&
                isFavorite == that.isFavorite &&
                Objects.equals(title, that.title) &&
                Objects.equals(description, that.description) &&
                Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, description, imageResId, url, isFavorite);
    }
}