
package com.adobe.aem.guides.demo.core.servlets;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.aem.guides.demo.core.services.Task15PageUpdateService;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.paths=/bin/task15/update",
        "sling.servlet.methods=POST"
    }
)
public class Task15PageUpdateServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG =
            LoggerFactory.getLogger(Task15PageUpdateServlet.class);

    @Reference
    private Task15PageUpdateService pageUpdateService;

    @Override
    protected void doPost(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        LOG.info("Task-15: Servlet execution started.");

        try {
            String result = pageUpdateService.updatePageProperties();

            if (result != null && result.startsWith("SUCCESS")) {
                response.setStatus(
                    SlingHttpServletResponse.SC_OK
                );

                response.getWriter().write(
                    "{\"status\":\"SUCCESS\",\"message\":\"Page properties updated successfully.\"}"
                );

                LOG.info("Task-15: Servlet execution completed successfully.");

            } else {
                response.setStatus(
                    SlingHttpServletResponse.SC_BAD_REQUEST
                );

                response.getWriter().write(
                    "{\"status\":\"FAILED\",\"message\":\""
                    + escapeJson(result)
                    + "\"}"
                );

                LOG.warn("Task-15: Page update failed: {}", result);
            }

        } catch (Exception e) {

            LOG.error("Task-15: Unexpected servlet error.", e);

            response.setStatus(
                SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            response.getWriter().write(
                "{\"status\":\"ERROR\",\"message\":\"Unexpected server error.\"}"
            );
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "Unknown error";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
