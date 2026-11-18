
package com.adobe.aem.guides.demo.core.services.impl;

import com.adobe.aem.guides.demo.core.models.WordPressArticle;
import com.adobe.aem.guides.demo.core.models.WordPressPage;
import com.adobe.aem.guides.demo.core.services.WordPressMigrationApiService;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;

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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.List;

@Component(
        service = WordPressMigrationApiService.class,
        immediate = true
)
@Designate(ocd = WordPressMigrationApiServiceImpl.Config.class)
public class WordPressMigrationApiServiceImpl
        implements WordPressMigrationApiService {

    private static final Logger LOG =
            LoggerFactory.getLogger(
                    WordPressMigrationApiServiceImpl.class);

    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 20000;
    private static final int MAX_PER_PAGE = 100;

    @ObjectClassDefinition(
            name = "WordPress Migration API Configuration")
    public @interface Config {

        @AttributeDefinition(name = "WordPress API URL")
        String apiUrl() default
                "https://wordpress.org/news/wp-json/wp/v2/posts";
    }

    private String apiUrl;

    @Activate
    protected void activate(Config config) {

        this.apiUrl = config.apiUrl();

        LOG.info(
                "WordPress Migration API Service activated. API URL: {}",
                apiUrl);
    }

    @Override
    public WordPressPage fetchArticles(int page, int perPage) {

        HttpURLConnection connection = null;

        // 1. Validate pagination parameters
        if (page < 1) {
            throw new IllegalArgumentException(
                    "Page number must be greater than zero");
        }

        if (perPage < 1 || perPage > MAX_PER_PAGE) {
            throw new IllegalArgumentException(
                    "Articles per page must be between 1 and 100");
        }

        if (apiUrl == null || apiUrl.trim().isEmpty()) {
            throw new IllegalStateException(
                    "WordPress API URL is not configured");
        }

        try {

            // 2. Construct WordPress API URL
            String separator = apiUrl.contains("?") ? "&" : "?";

            String requestUrl = apiUrl
                    + separator
                    + "per_page=" + perPage
                    + "&page=" + page
                    + "&_fields=id,date,slug,link,title,content,excerpt";

            LOG.info(
                    "Requesting WordPress articles. Page: {}, Per Page: {}",
                    page,
                    perPage);

            URL url = new URL(requestUrl);

            connection =
                    (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");

            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);

            connection.setRequestProperty(
                    "Accept",
                    "application/json");

            connection.setRequestProperty(
                    "User-Agent",
                    "AEM-WordPress-Migration/1.0");

            // 3. Get HTTP response
            int status = connection.getResponseCode();

            if (status != HttpURLConnection.HTTP_OK) {

                String errorBody = readErrorResponse(connection);

                LOG.error(
                        "WordPress API failed. Page: {}, HTTP Status: {}, Response: {}",
                        page,
                        status,
                        errorBody);

                throw new IllegalStateException(
                        "WordPress API request failed. "
                                + "Page: " + page
                                + ", HTTP Status: " + status
                                + ", Response: " + errorBody);
            }

            // 4. Read WordPress pagination headers
            int totalPosts = parseHeader(
                    connection.getHeaderField("X-WP-Total"),
                    "X-WP-Total",
                    page);

            int totalPages = parseHeader(
                    connection.getHeaderField("X-WP-TotalPages"),
                    "X-WP-TotalPages",
                    page);

            // 5. Read API response body
            StringBuilder response = new StringBuilder();

            try (
                    InputStream input = connection.getInputStream();

                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            input,
                                            StandardCharsets.UTF_8))
            ) {

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            // 6. Validate response body
            if (response.length() == 0) {
                throw new IllegalStateException(
                        "WordPress API returned an empty response body "
                                + "for page " + page);
            }

            // 7. Parse JSON response
            JSONArray jsonArray;

            try {
                jsonArray = new JSONArray(response.toString());

            } catch (Exception e) {

                throw new IllegalStateException(
                        "Invalid JSON returned by WordPress API "
                                + "for page " + page,
                        e);
            }

            List<WordPressArticle> articles =
                    new ArrayList<>();

            // 8. Convert WordPress JSON into Java objects
            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject post =
                        jsonArray.getJSONObject(i);

                JSONObject title =
                        post.optJSONObject("title");

                JSONObject excerpt =
                        post.optJSONObject("excerpt");

                JSONObject content =
                        post.optJSONObject("content");

                String id = post.optString("id");

                if (id == null ||
                        id.trim().isEmpty() ||
                        "null".equals(id)) {

                    throw new IllegalStateException(
                            "WordPress article ID is missing "
                                    + "on page " + page
                                    + ", article index " + i);
                }

                WordPressArticle article =
                        new WordPressArticle();

                article.setWordpressId(id);

                article.setTitle(
                        title != null
                                ? title.optString("rendered")
                                : "");

                article.setDescription(
                        excerpt != null
                                ? excerpt.optString("rendered")
                                : "");

                article.setContent(
                        content != null
                                ? content.optString("rendered")
                                : "");

                article.setPublishedDate(
                        post.optString("date"));

                article.setSourceUrl(
                        post.optString("link"));

                article.setSlug(
                        post.optString("slug"));

                articles.add(article);
            }

            // 9. Build result
            WordPressPage result = new WordPressPage();

            result.setArticles(articles);
            result.setTotalPosts(totalPosts);
            result.setTotalPages(totalPages);
            result.setCurrentPage(page);

            LOG.info(
                    "WordPress API successful. Page: {}, Articles: {}, Total Posts: {}, Total Pages: {}",
                    page,
                    articles.size(),
                    totalPosts,
                    totalPages);

            return result;

        } catch (Exception e) {

            LOG.error(
                    "WordPress API retrieval failed for page {}: {}",
                    page,
                    e.getMessage(),
                    e);

            // Do not return an empty page on API failure.
            // Propagate the failure so the Sling JobConsumer
            // can identify and handle the failed page.
            throw new RuntimeException(
                    "Failed to fetch WordPress articles from page "
                            + page + ": " + e.getMessage(),
                    e);

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Parses WordPress pagination headers.
     */
    private int parseHeader(
            String value,
            String headerName,
            int page) {

        if (value == null || value.trim().isEmpty()) {

            throw new IllegalStateException(
                    "Missing WordPress pagination header: "
                            + headerName
                            + " on page " + page);
        }

        try {

            return Integer.parseInt(value);

        } catch (NumberFormatException e) {

            throw new IllegalStateException(
                    "Invalid WordPress pagination header: "
                            + headerName
                            + ", Value: " + value,
                    e);
        }
    }

    /**
     * Reads the error response returned by WordPress.
     */
    private String readErrorResponse(
            HttpURLConnection connection) {

        InputStream errorStream =
                connection.getErrorStream();

        if (errorStream == null) {
            return "No error response body";
        }

        try (
                InputStream input = errorStream;
                ByteArrayOutputStream output =
                        new ByteArrayOutputStream()
        ) {

            byte[] buffer = new byte[1024];

            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }

            String response =
                    new String(
                            output.toByteArray(),
                            StandardCharsets.UTF_8);

            // Keep log messages manageable.
            if (response.length() > 2000) {
                response = response.substring(0, 2000);
            }

            return response;

        } catch (IOException e) {

            LOG.warn(
                    "Could not read WordPress API error response",
                    e);

            return "Could not read error response: "
                    + e.getMessage();
        }
    }
}
