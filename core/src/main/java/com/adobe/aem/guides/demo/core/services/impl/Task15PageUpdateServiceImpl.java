
package com.adobe.aem.guides.demo.core.services.impl;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.aem.guides.demo.core.services.Task15ConfigService;
import com.adobe.aem.guides.demo.core.services.Task15PageUpdateService;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

@Component(
    service = Task15PageUpdateService.class,
    immediate = true
)
public class Task15PageUpdateServiceImpl
        implements Task15PageUpdateService {

    private static final Logger LOG =
            LoggerFactory.getLogger(Task15PageUpdateServiceImpl.class);

    private static final String SUBSERVICE = "task15-service";

    @Reference
    private Task15ConfigService configService;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Override
    public String updatePageProperties() {

        // Step 1: Read OSGi configuration values
        String pagePath = configService.getPagePath();
        String clientId = configService.getClientId();
        String apiToken = configService.getApiToken();

        // Step 2: Validate configuration values
        if (isBlank(pagePath)
                || isBlank(clientId)
                || isBlank(apiToken)) {

            LOG.error("Task-15: Configuration values are missing.");

            return "FAILED: Required configuration values are missing.";
        }

        // Step 3: Validate page path format
        if (!isValidPagePath(pagePath)) {

            LOG.error("Task-15: Invalid page path format: {}", pagePath);

            return "FAILED: Invalid page path format.";
        }

        // Step 4: Prepare service user authentication
        Map<String, Object> authInfo = new HashMap<>();

        authInfo.put(
            ResourceResolverFactory.SUBSERVICE,
            SUBSERVICE
        );

        // Step 5: Obtain service resource resolver
        try (ResourceResolver resolver =
                resolverFactory.getServiceResourceResolver(authInfo)) {

            LOG.info("Task-15: Service Resource Resolver obtained.");

            // Step 6: Obtain PageManager
            PageManager pageManager =
                    resolver.adaptTo(PageManager.class);

            if (pageManager == null) {

                LOG.error("Task-15: Unable to obtain PageManager.");

                return "FAILED: Unable to obtain PageManager.";
            }

            // Step 7: Validate the configured AEM page
            Page page = pageManager.getPage(pagePath);

            if (page == null) {

                LOG.error(
                    "Task-15: Configured path is not an AEM page: {}",
                    pagePath
                );

                return "FAILED: Configured path is not a valid AEM page.";
            }

            LOG.info(
                "Task-15: Valid AEM page identified: {}",
                page.getPath()
            );

            // Step 8: Obtain page content resource
            Resource contentResource = page.getContentResource();

            if (contentResource == null) {

                LOG.error(
                    "Task-15: Page content resource not found: {}",
                    pagePath
                );

                return "FAILED: Page content resource not found.";
            }

            // Step 9: Obtain JCR session
            Session session = resolver.adaptTo(Session.class);

            if (session == null) {

                LOG.error("Task-15: Unable to obtain JCR session.");

                return "FAILED: Unable to obtain JCR session.";
            }

            // Step 10: Get jcr:content node
            Node contentNode = session.getNode(
                contentResource.getPath()
            );

            // Step 11: Update configured properties
            contentNode.setProperty("clientId", clientId);

            contentNode.setProperty("apiToken", apiToken);

            // Step 12: Save repository changes
            session.save();

            LOG.info(
                "Task-15: Successfully updated page properties for {}",
                pagePath
            );

            return "SUCCESS: Page properties updated successfully.";

        } catch (LoginException e) {

            LOG.error(
                "Task-15: Service user authentication failed.",
                e
            );

            return "FAILED: Service user authentication failed.";

        } catch (RepositoryException e) {

            LOG.error(
                "Task-15: Repository operation failed.",
                e
            );

            return "FAILED: Repository operation failed.";
        }
    }

    // Validate page path format
    private boolean isValidPagePath(String pagePath) {

        if (isBlank(pagePath)) {
            return false;
        }

        if (!pagePath.startsWith("/content/")) {
            return false;
        }

        if (pagePath.contains("//")) {
            return false;
        }

        String[] segments = pagePath.split("/");

        for (String segment : segments) {

            if ("..".equals(segment) || ".".equals(segment)) {
                return false;
            }
        }

        return true;
    }

    // Check empty or null values
    private boolean isBlank(String value) {

        return value == null || value.trim().isEmpty();
    }
}
