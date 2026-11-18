package com.adobe.aem.guides.demo.core.services.impl;

import com.adobe.aem.guides.demo.core.services.WordPressService;

import org.apache.sling.commons.json.JSONArray;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.AttributeDefinition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component(service = WordPressService.class)
@Designate(ocd = WordPressServiceImpl.Config.class)
public class WordPressServiceImpl implements WordPressService {

    private static final Logger LOG =
        LoggerFactory.getLogger(WordPressServiceImpl.class);

    @ObjectClassDefinition(name = "WordPress API Configuration")
    public @interface Config {

        @AttributeDefinition(name = "WordPress API URL")
        String apiUrl() default
            "https://wordpress.org/news/wp-json/wp/v2/posts";
    }

    private String apiUrl;

    @Activate
    protected void activate(Config config) {
        this.apiUrl = config.apiUrl();
    }

    @Override
    public JSONArray getPosts(int limit) {

        HttpURLConnection connection = null;

        try {

            String requestUrl = apiUrl
                + "?per_page=" + Math.max(1, Math.min(limit, 20))
                + "&_fields=id,date,link,title,excerpt";

            URL url = new URL(requestUrl);

            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty(
                "Accept", "application/json"
            );

            int status = connection.getResponseCode();

            if (status != HttpURLConnection.HTTP_OK) {
                LOG.error("WordPress API returned status: {}", status);
                return new JSONArray();
            }

            StringBuilder response = new StringBuilder();

            try (InputStream input = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(
                     new InputStreamReader(
                         input, StandardCharsets.UTF_8
                     ))) {

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            return new JSONArray(response.toString());

        } catch (Exception e) {

            LOG.error("Unable to fetch WordPress articles", e);

            return new JSONArray();

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}