
package com.adobe.aem.guides.demo.core.servlets;

import com.adobe.aem.guides.demo.core.models.WordPressArticle;
import com.adobe.aem.guides.demo.core.models.WordPressPage;
import com.adobe.aem.guides.demo.core.services.WordPressMigrationApiService;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import java.io.IOException;
import java.util.List;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.paths=/bin/wordpress/test",
        "sling.servlet.methods=GET"
    }
)
public class WordPressApiTestServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;

    @Reference
    private WordPressMigrationApiService wordPressMigrationApiService;

    @Override
    protected void doGet(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        int page = getPositiveParameter(request, "page", 1);
        int perPage = getPositiveParameter(request, "perPage", 5);

        if (perPage > 100) {
            response.setStatus(
                SlingHttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(
                "{\"status\":\"error\",\"message\":\"perPage cannot exceed 100\"}");
            return;
        }

        WordPressPage result =
                wordPressMigrationApiService.fetchArticles(page, perPage);

        List<WordPressArticle> articles = result.getArticles();

        StringBuilder json = new StringBuilder();

        json.append("{");
        json.append("\"status\":\"success\",");
        json.append("\"totalPosts\":").append(result.getTotalPosts()).append(",");
        json.append("\"totalPages\":").append(result.getTotalPages()).append(",");
        json.append("\"currentPage\":").append(result.getCurrentPage()).append(",");
        json.append("\"articleCount\":").append(
                articles != null ? articles.size() : 0).append(",");
        json.append("\"articles\":[");

        if (articles != null) {
            for (int i = 0; i < articles.size(); i++) {

                WordPressArticle article = articles.get(i);

                if (i > 0) {
                    json.append(",");
                }

                json.append("{");
                json.append("\"wordpressId\":\"")
                    .append(escapeJson(article.getWordpressId())).append("\",");
                json.append("\"title\":\"")
                    .append(escapeJson(article.getTitle())).append("\",");
                json.append("\"slug\":\"")
                    .append(escapeJson(article.getSlug())).append("\"");
                json.append("}");
            }
        }

        json.append("]}");

        response.getWriter().write(json.toString());
    }

    private int getPositiveParameter(
            SlingHttpServletRequest request,
            String name,
            int defaultValue) {

        try {
            int value = Integer.parseInt(request.getParameter(name));
            return value > 0 ? value : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String escapeJson(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
