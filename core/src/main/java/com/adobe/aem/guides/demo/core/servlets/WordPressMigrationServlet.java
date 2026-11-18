
package com.adobe.aem.guides.demo.core.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;

import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component(
        service = Servlet.class,
        property = {
                "sling.servlet.paths=/bin/wordpress/migrate",
                "sling.servlet.methods=POST"
        }
)
public class WordPressMigrationServlet
        extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG =
            LoggerFactory.getLogger(
                    WordPressMigrationServlet.class);

    // Sling Job topic
    private static final String JOB_TOPIC =
            "com/aem/wordpress/migration";

    // Default articles per API request
    private static final int DEFAULT_PER_PAGE = 100;

    // Maximum articles allowed per API request
    private static final int MAX_PER_PAGE = 100;

    @Reference
    private JobManager jobManager;

    @Override
    protected void doPost(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {

            // 1. Read articles per page
            String perPageParameter =
                    request.getParameter("perPage");

            int perPage = DEFAULT_PER_PAGE;

            if (perPageParameter != null &&
                    !perPageParameter.trim().isEmpty()) {

                perPage = Integer.parseInt(
                        perPageParameter.trim());
            }

            // 2. Validate articles per page
            if (perPage < 1 || perPage > MAX_PER_PAGE) {

                writeError(
                        response,
                        400,
                        "perPage must be between 1 and 100");

                return;
            }

            // 3. Read maximum pages
            String maxPagesParameter =
                    request.getParameter("maxPages");

            int maxPages = 0;

            if (maxPagesParameter != null &&
                    !maxPagesParameter.trim().isEmpty()) {

                maxPages = Integer.parseInt(
                        maxPagesParameter.trim());
            }

            // 4. Validate maximum pages
            if (maxPages < 0) {

                writeError(
                        response,
                        400,
                        "maxPages cannot be negative");

                return;
            }

            // 5. Generate unique migration ID
            String migrationId =
                    UUID.randomUUID().toString();

            // 6. Prepare first Sling Job properties
            Map<String, Object> jobProperties =
                    new HashMap<>();

            jobProperties.put(
                    "migrationId",
                    migrationId);

            jobProperties.put(
                    "page",
                    1);

            jobProperties.put(
                    "perPage",
                    perPage);

            jobProperties.put(
                    "maxPages",
                    maxPages);

            // 7. Submit the first migration job
            Job firstJob =
                    jobManager.addJob(
                            JOB_TOPIC,
                            jobProperties);

            if (firstJob == null) {

                throw new IllegalStateException(
                        "Unable to schedule WordPress migration job");
            }

            // 8. Log migration details
            LOG.info(
                    "WordPress migration job scheduled successfully. "
                            + "Migration ID: {}, "
                            + "Per Page: {}, "
                            + "Max Pages: {}",
                    migrationId,
                    perPage,
                    maxPages);

            // 9. Return HTTP 202 Accepted
            response.setStatus(
                    SlingHttpServletResponse.SC_ACCEPTED);

            response.getWriter().write(
                    "{"
                            + "\"status\":\"accepted\","
                            + "\"message\":\"WordPress migration job scheduled\","
                            + "\"migrationId\":\"" + migrationId + "\","
                            + "\"startingPage\":1,"
                            + "\"perPage\":" + perPage + ","
                            + "\"maxPages\":" + maxPages
                            + "}");

        } catch (NumberFormatException e) {

            LOG.warn(
                    "Invalid numeric migration parameter: {}",
                    e.getMessage());

            writeError(
                    response,
                    400,
                    "Invalid numeric parameter. "
                            + "Check perPage and maxPages.");

        } catch (Exception e) {

            LOG.error(
                    "Could not start WordPress bulk migration",
                    e);

            writeError(
                    response,
                    500,
                    "Could not start WordPress migration. "
                            + "Check AEM error.log.");
        }
    }

    /**
     * Writes a JSON error response.
     */
    private void writeError(
            SlingHttpServletResponse response,
            int status,
            String message) throws IOException {

        response.setStatus(status);

        response.getWriter().write(
                "{"
                        + "\"status\":\"error\","
                        + "\"message\":\"" + message + "\""
                        + "}");
    }
}
