package com.qimiao.social.callback;


import java.util.Objects;

public class OItem {
    private String title;
    private String sortingIndex;

    public OItem() {
    }

    public OItem(String title, String sortingIndex) {
        this.title = title;
        this.sortingIndex = sortingIndex;
    }

    public String getTitle() {
        return title;
    }

    public String getSortingIndex() {
        return sortingIndex;
    }

    public void setSortingIndex(String sortingIndex) {
        this.sortingIndex = sortingIndex;
    }

    @Override
    public String toString() {
        return title + " (" + sortingIndex + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OItem oItem = (OItem) o;
        return Objects.equals(title, oItem.title) &&
                Objects.equals(sortingIndex, oItem.sortingIndex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, sortingIndex);
    }
}