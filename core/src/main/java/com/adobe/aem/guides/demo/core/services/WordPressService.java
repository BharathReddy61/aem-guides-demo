package com.adobe.aem.guides.demo.core.services;

import org.apache.sling.commons.json.JSONArray;

public interface WordPressService {

    JSONArray getPosts(int limit);

}
