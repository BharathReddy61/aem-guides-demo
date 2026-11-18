package com.adobe.aem.guides.demo.core.models;

import java.util.List;

public class WordPressPage {

    private List<WordPressArticle> articles;
    private int totalPosts;
    private int totalPages;
    private int currentPage;

    public List<WordPressArticle> getArticles() {
        return articles;
    }

    public void setArticles(List<WordPressArticle> articles) {
        this.articles = articles;
    }

    public int getTotalPosts() {
        return totalPosts;
    }

    public void setTotalPosts(int totalPosts) {
        this.totalPosts = totalPosts;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }
}