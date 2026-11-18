package com.adobe.aem.guides.demo.core.models;

public class WordPressArticle {

    private String wordpressId;
    private String title;
    private String description;
    private String content;
    private String publishedDate;
    private String sourceUrl;
    private String slug;

    public String getWordpressId() {
        return wordpressId;
    }

    public void setWordpressId(String wordpressId) {
        this.wordpressId = wordpressId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(String publishedDate) {
        this.publishedDate = publishedDate;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}