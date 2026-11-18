
package com.adobe.aem.guides.demo.core.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Exporter;

import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

@Model(
    adaptables = {
        Resource.class,
        SlingHttpServletRequest.class
    },
    resourceType = "demo/components/task17-pagination",
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
@Exporter(
    name = "jackson",
    extensions = "json"
)
public class Task17PaginationModel {

    @ValueMapValue(name = "path")
    private String parentPagePath;

    @SlingObject
    private ResourceResolver resourceResolver;

    @SlingObject
    private SlingHttpServletRequest request;

    private List<String> pages = new ArrayList<>();

    private int pageLimit;

    private int pageSize;

    private int totalPages;

    private int totalResults;

    private int offset;

    private String message;

    @PostConstruct
    protected void init() {

        // Default values
        pageLimit = 3;
        pageSize = 1;

        // Read request parameters
        if (request != null) {

            pageLimit = getPositiveParameter(
                request.getParameter("pageLimit"), 3
            );

            pageSize = getPositiveParameter(
                request.getParameter("pageSize"), 1
            );
        }

        // Validate authored path
        if (parentPagePath == null || parentPagePath.trim().isEmpty()) {
            message = "Please author a parent page path.";
            return;
        }

        // Adapt ResourceResolver to PageManager
        PageManager pageManager =
            resourceResolver.adaptTo(PageManager.class);

        if (pageManager == null) {
            message = "Unable to obtain PageManager.";
            return;
        }

        // Retrieve authored parent page
        Page parentPage = pageManager.getPage(parentPagePath);

        if (parentPage == null) {
            message = "Parent page does not exist: " + parentPagePath;
            return;
        }

        // Collect parent page and its direct child pages
        List<String> allPages = new ArrayList<>();

        allPages.add(parentPage.getPath());

        Iterator<Page> childPages = parentPage.listChildren();

        while (childPages.hasNext()) {

            Page childPage = childPages.next();

            allPages.add(childPage.getPath());
        }

        // Total available pages
        totalResults = allPages.size();

        // Calculate pagination offset
        offset = (pageSize - 1) * pageLimit;

        // Calculate total number of pagination pages
        totalPages = (int) Math.ceil(
            (double) totalResults / pageLimit
        );

        // Extract requested page range
        int start = Math.min(offset, totalResults);

        int end = Math.min(
            start + pageLimit,
            totalResults
        );

        pages = new ArrayList<>(
            allPages.subList(start, end)
        );

        message = "Pagination completed successfully.";
    }

    private int getPositiveParameter(String value, int defaultValue) {

        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        try {

            int parsedValue = Integer.parseInt(value);

            if (parsedValue > 0) {
                return parsedValue;
            }

        } catch (NumberFormatException e) {
            // Use default value for invalid input
        }

        return defaultValue;
    }

    public List<String> getPages() {
        return Collections.unmodifiableList(pages);
    }

    public String getParentPagePath() {
        return parentPagePath;
    }

    public int getPageLimit() {
        return pageLimit;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getTotalResults() {
        return totalResults;
    }

    public int getOffset() {
        return offset;
    }

    public String getMessage() {
        return message;
    }
}
