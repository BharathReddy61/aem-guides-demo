package com.adobe.aem.guides.demo.core.models;

import com.adobe.aem.guides.demo.core.services.WordPressService;

import org.apache.sling.api.resource.Resource;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;

import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import javax.annotation.PostConstruct;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import java.util.ArrayList;
import java.util.List;

@Model(
    adaptables = Resource.class,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class WordPressModel {

    @OSGiService
    private WordPressService wordPressService;

    @ValueMapValue
    private Integer limit;

    private List<Article> articles;

    @PostConstruct
    protected void init() {

        articles = new ArrayList<>();

        if (wordPressService == null) {
            return;
        }

        int articleLimit = limit != null
            ? Math.max(1, Math.min(limit, 20))
            : 5;

        JSONArray response =
            wordPressService.getPosts(articleLimit);

        for (int i = 0; i < response.length(); i++) {

            try {

                JSONObject post = response.getJSONObject(i);

                JSONObject title = post.getJSONObject("title");
                JSONObject excerpt = post.getJSONObject("excerpt");

                Article article = new Article();

                article.setId(post.optInt("id"));
                article.setTitle(title.optString("rendered"));
                article.setExcerpt(excerpt.optString("rendered"));
                article.setDate(post.optString("date"));
                article.setLink(post.optString("link"));

                articles.add(article);

            } catch (Exception e) {
                // Skip malformed article.
            }
        }
    }

    public List<Article> getArticles() {
        return articles;
    }

    public static class Article {

        private int id;
        private String title;
        private String excerpt;
        private String date;
        private String link;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getExcerpt() {
            return excerpt;
        }

        public void setExcerpt(String excerpt) {
            this.excerpt = excerpt;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }
    }
}