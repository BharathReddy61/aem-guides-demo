
package com.adobe.aem.guides.demo.core.servlets;

import com.adobe.aem.guides.demo.core.models.WordPressArticle;
import com.adobe.aem.guides.demo.core.models.WordPressPage;
import com.adobe.aem.guides.demo.core.services.WordPressMigrationApiService;
import com.adobe.aem.guides.demo.core.services.WordPressContentFragmentService;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.paths=/bin/wordpress/create-test-fragment",
        "sling.servlet.methods=GET"
    }
)
public class WordPressContentFragmentTestServlet
        extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private WordPressMigrationApiService apiService;

    @Reference
    private WordPressContentFragmentService fragmentService;

    @Override
    protected void doGet(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> params = new HashMap<>();

        params.put(
            ResourceResolverFactory.SUBSERVICE,
            "wordpress-migration"
        );

        try (ResourceResolver resolver =
                resolverFactory.getServiceResourceResolver(params)) {

            // Fetch exactly one article from WordPress
            WordPressPage page = apiService.fetchArticles(1, 1);

            List<WordPressArticle> articles = page.getArticles();

            if (articles == null || articles.isEmpty()) {
                response.setStatus(502);
                response.getWriter().write(
                    "{\"status\":\"error\",\"message\":\"No articles returned by WordPress API. Check AEM error.log.\"}"
                );
                return;
            }

            WordPressArticle article = articles.get(0);

            // Create the Content Fragment
            String fragmentPath =
                    fragmentService.createContentFragment(
                            resolver, article);

            response.getWriter().write(
                "{"
                + "\"status\":\"success\","
                + "\"wordpressId\":\"" + escape(article.getWordpressId()) + "\","
                + "\"title\":\"" + escape(article.getTitle()) + "\","
                + "\"fragmentPath\":\"" + escape(fragmentPath) + "\""
                + "}"
            );

        } catch (Exception e) {

            response.setStatus(500);

            response.getWriter().write(
                "{"
                + "\"status\":\"error\","
                + "\"message\":\"" + escape(e.getMessage()) + "\""
                + "}"
            );
        }
    }

    private String escape(String value) {

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
