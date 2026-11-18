
package com.adobe.aem.guides.demo.core.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.paths=/bin/wordpress/permission-test",
        "sling.servlet.methods=GET"
    }
)
public class WordPressPermissionTestServlet
        extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final String MODEL_PATH =
    "/conf/demo/settings/dam/cfm/models/wordpress-article-model";

    private static final String DESTINATION_PATH =
        "/content/dam/demo/wordpress-migration";

    @Reference
    private ResourceResolverFactory resolverFactory;

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

            Session session = resolver.adaptTo(Session.class);

            if (session == null) {
                response.setStatus(
                    SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR
                );

                response.getWriter().write(
                    "{\"status\":\"error\","
                    + "\"message\":\"Could not adapt resolver to JCR Session\"}"
                );
                return;
            }

            Resource modelResource =
                resolver.getResource(MODEL_PATH);

            Resource destinationResource =
                resolver.getResource(DESTINATION_PATH);

                // List all model nodes visible to the service user
Resource modelsFolder = resolver.getResource(
    "/conf/demo/settings/dam/cfm/models"
);

StringBuilder modelChildren = new StringBuilder();

if (modelsFolder != null) {

    for (Resource child : modelsFolder.getChildren()) {

        if (modelChildren.length() > 0) {
            modelChildren.append(",");
        }

        modelChildren.append("\"")
            .append(escapeJson(child.getName()))
            .append("\"");
    }
}

            // Check each level of the model path
            String[] paths = {
                "/conf",
                "/conf/demo",
                "/conf/demo/settings",
                "/conf/demo/settings/dam",
                "/conf/demo/settings/dam/cfm",
                "/conf/demo/settings/dam/cfm/models",
                "/conf/demo/settings/dam/cfm/models/wordpress-article"
            };

            StringBuilder modelPathResults =
                new StringBuilder();

            for (int i = 0; i < paths.length; i++) {

                Resource resource =
                    resolver.getResource(paths[i]);

                if (i > 0) {
                    modelPathResults.append(",");
                }

                modelPathResults.append("\"")
                    .append(escapeJson(paths[i]))
                    .append("\":")
                    .append(resource != null);
            }

            StringBuilder result = new StringBuilder();

            result.append("{");

            result.append("\"status\":\"success\",");

            result.append("\"serviceUser\":\"")
                .append(escapeJson(session.getUserID()))
                .append("\",");

            result.append("\"modelPath\":\"")
                .append(escapeJson(MODEL_PATH))
                .append("\",");

            result.append("\"confRead\":")
                .append(
                    session.hasPermission("/conf/demo", "read")
                )
                .append(",");

            result.append("\"modelRead\":")
                .append(
                    session.hasPermission(MODEL_PATH, "read")
                )
                .append(",");

            result.append("\"modelResourceExists\":")
                .append(modelResource != null)
                .append(",");

            result.append("\"damRead\":")
                .append(
                    session.hasPermission(
                        "/content/dam/demo", "read")
                )
                .append(",");

            result.append("\"migrationFolderRead\":")
                .append(
                    session.hasPermission(
                        DESTINATION_PATH, "read")
                )
                .append(",");

            result.append("\"migrationFolderCreate\":")
                .append(
                    session.hasPermission(
                        DESTINATION_PATH + "/permission-test",
                        "add_node")
                )
                .append(",");

            result.append("\"migrationFolderModify\":")
                .append(
                    session.hasPermission(
                        DESTINATION_PATH, "set_property")
                )
                .append(",");

            result.append("\"destinationResourceExists\":")
                .append(destinationResource != null)
                .append(",");

            result.append("\"modelPathChecks\":{")
                .append(modelPathResults)
                .append("}");

                result.append("\"availableModelNodes\":[")
      .append(modelChildren)
      .append("],");
            result.append("}");

            response.getWriter().write(result.toString());

        } catch (Exception e) {

            response.setStatus(
                SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            response.getWriter().write(
                "{"
                + "\"status\":\"error\","
                + "\"message\":\""
                + escapeJson(e.getMessage())
                + "\""
                + "}"
            );
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
