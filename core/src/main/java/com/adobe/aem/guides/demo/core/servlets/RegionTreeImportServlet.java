package com.adobe.aem.guides.demo.core.servlets;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.aem.guides.demo.core.utils.RegionTreeUtils;

/**
 * Region Tree Import Servlet.
 *
 * Endpoint: POST /bin/regiontree/import
 *
 * Flow:
 * 1. Read and validate the JSON payload.
 * 2. Create /conf/region-tree if missing.
 * 3. Delete and recreate /conf/region-tree/temp.
 * 4. Build the region > area > resort hierarchy under temp.
 * 5. Create or update /conf/region-tree/data.
 * 6. Save, then fire the comparator job (com/jet2/region-tree/temp).
 */
@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.paths=/bin/regiontree/import",
        "sling.servlet.methods=POST"
    }
)
public class RegionTreeImportServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG =
            LoggerFactory.getLogger(RegionTreeImportServlet.class);

    private static final String SUBSERVICE = "task19-region-tree";

    private static final String CONF_PATH = "/conf";
    private static final String REGION_TREE_PATH = "/conf/region-tree";
    private static final String TEMP_PATH = "/conf/region-tree/temp";
    private static final String DATA_PATH = "/conf/region-tree/data";
    private static final String ACTUAL_PATH = "/conf/region-tree/actual";

    private static final String JOB_TOPIC = "com/jet2/region-tree/temp";

    private static final String MSG_SUCCESS =
            "The temp folder is created correctly!!";
    private static final String MSG_FAILURE =
            "Error during the creation of the temp folder!!!";

    @Reference
    private transient ResourceResolverFactory resolverFactory;

    @Reference
    private transient JobManager jobManager;

    @Override
    protected void doPost(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        LOG.info("========== TASK-19 REGION TREE IMPORT STARTED ==========");

        try {

            // STEP 1: Read incoming JSON.
            String rawJson = readRequestBody(request);

            if (rawJson == null || rawJson.trim().isEmpty()) {
                LOG.error("Task-19: Request body is empty.");
                writeResponse(response,
                        SlingHttpServletResponse.SC_BAD_REQUEST,
                        "Request body is empty.");
                return;
            }

            LOG.info("Task-19: Incoming JSON received. Length: {}",
                    rawJson.length());

            // STEP 2: Parse and validate JSON.
            JSONObject payload = new JSONObject(rawJson);

            if (payload.optJSONArray("regions") == null) {
                throw new JSONException(
                        "Invalid payload. Expected regions array.");
            }

            // STEP 3: Obtain service ResourceResolver.
            Map<String, Object> authInfo = new HashMap<>();
            authInfo.put(ResourceResolverFactory.SUBSERVICE, SUBSERVICE);

            // Closing the resolver logs out the session, which discards any
            // unsaved changes if an exception is thrown before session.save().
            try (ResourceResolver resolver =
                         resolverFactory.getServiceResourceResolver(authInfo)) {

                LOG.info("Task-19: Service ResourceResolver obtained.");

                Session session = resolver.adaptTo(Session.class);

                if (session == null) {
                    throw new RepositoryException(
                            "Unable to obtain JCR Session.");
                }

                LOG.info("Task-19: JCR Session obtained. User: {}",
                        session.getUserID());

                // STEP 4: Verify /conf.
                if (!session.nodeExists(CONF_PATH)) {
                    throw new RepositoryException(
                            "/conf is not accessible for the service user.");
                }

                Node confNode = session.getNode(CONF_PATH);

                // STEP 5: Create region-tree if missing.
                Node regionTreeNode;

                if (session.nodeExists(REGION_TREE_PATH)) {
                    regionTreeNode = session.getNode(REGION_TREE_PATH);
                    LOG.info("Task-19: Existing region-tree folder found.");
                } else {
                    regionTreeNode =
                            confNode.addNode("region-tree", "nt:unstructured");
                    LOG.info("Task-19: Created {}.", REGION_TREE_PATH);
                }

                // STEP 6: Delete old temp folder (fresh build every time).
                if (session.nodeExists(TEMP_PATH)) {
                    session.getNode(TEMP_PATH).remove();
                    LOG.info("Task-19: Existing temp folder deleted.");
                }

                // STEP 7: Create fresh temp folder.
                Node tempRoot =
                        regionTreeNode.addNode("temp", "nt:unstructured");

                LOG.info("Task-19: Fresh temp folder created.");

                // STEP 8: Record import start time.
                String startDateImport = java.time.Instant.now().toString();

                // STEP 9: Build region hierarchy.
                RegionTreeUtils.buildTempTree(tempRoot, payload);

                LOG.info("Task-19: Region hierarchy created successfully.");

                // STEP 10: Record import end time.
                String endDateImport = java.time.Instant.now().toString();

                // STEP 11: Create or update metadata node.
                Node dataNode;

                if (session.nodeExists(DATA_PATH)) {
                    dataNode = session.getNode(DATA_PATH);
                } else {
                    dataNode = regionTreeNode.addNode("data", "nt:unstructured");
                }

                dataNode.setProperty("regionTreeJson", rawJson);
                dataNode.setProperty("startDateImport", startDateImport);
                dataNode.setProperty("endDateImport", endDateImport);

                LOG.info("Task-19: Import metadata stored.");

                // STEP 12: Persist repository changes (before firing the job).
                session.save();

                LOG.info("Task-19: Repository changes saved successfully.");

                // STEP 13: Submit comparison job.
                Map<String, Object> jobProperties = new HashMap<>();
                jobProperties.put("tempPath", TEMP_PATH);
                jobProperties.put("actualPath", ACTUAL_PATH);
                jobProperties.put("importStart", startDateImport);
                jobProperties.put("importEnd", endDateImport);

                Job job = jobManager.addJob(JOB_TOPIC, jobProperties);

                if (job == null) {
                    LOG.error("Task-19: Comparison job submission failed.");
                    writeResponse(response,
                            SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "Temp folder created, but comparison job submission failed.");
                    return;
                }

                LOG.info("Task-19: Comparison job submitted. Job ID: {}",
                        job.getId());
                LOG.info("========== TASK-19 REGION TREE IMPORT COMPLETED ==========");

                // STEP 14: Return success response.
                writeResponse(response,
                        SlingHttpServletResponse.SC_OK, MSG_SUCCESS);
            }

        } catch (JSONException e) {

            LOG.error("Task-19: Invalid JSON payload.", e);

            writeResponse(response,
                    SlingHttpServletResponse.SC_BAD_REQUEST,
                    "Invalid JSON payload.");

        } catch (Exception e) {

            LOG.error("Task-19: Error during region tree import.", e);

            if (!response.isCommitted()) {
                writeResponse(response,
                        SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        MSG_FAILURE);
            }
        }
    }

    /**
     * Reads the complete HTTP request body.
     */
    private String readRequestBody(SlingHttpServletRequest request)
            throws IOException {

        StringBuilder builder = new StringBuilder();

        try (Reader reader = request.getReader()) {

            char[] buffer = new char[4096];
            int read;

            while ((read = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, read);
            }
        }

        return builder.toString();
    }

    /**
     * Writes a JSON servlet response: {"status": ..., "message": ...}.
     */
    private void writeResponse(
            SlingHttpServletResponse response,
            int status,
            String message)
            throws IOException {

        response.setStatus(status);

        try {
            JSONObject result = new JSONObject();
            result.put("status", status);
            result.put("message", message);

            response.getWriter().write(result.toString());

        } catch (JSONException e) {
            throw new IOException("Unable to generate servlet response.", e);
        }
    }
}