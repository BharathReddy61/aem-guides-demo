package com.adobe.aem.guides.demo.core.services;

import com.adobe.aem.guides.demo.core.models.WordPressPage;

public interface WordPressMigrationApiService {

    WordPressPage fetchArticles(int page, int perPage);

}