package com.adobe.aem.guides.jobs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.aem.guides.demo.core.utils.RegionTreeUtils;

/**
 * Region Tree Comparator.
 *
 * Topic: com/jet2/region-tree/temp
 *
 * Compares /conf/region-tree/temp with /conf/region-tree/actual and builds
 * the created / updated / moved lists, then fires
 * com/jet2/region-tree/update and deletes temp.
 */
@Component(
    service = JobConsumer.class,
    property = {
        JobConsumer.PROPERTY_TOPICS + "=com/jet2/region-tree/temp"
    }
)
public class RegionTreeComparatorJobConsumer implements JobConsumer {

    private static final Logger LOG =
            LoggerFactory.getLogger(RegionTreeComparatorJobConsumer.class);

    private static final String SUBSERVICE = "task19-region-tree";

    private static final String TEMP_PATH = "/conf/region-tree/temp";

    private static final String ACTUAL_PATH = "/conf/region-tree/actual";

    private static final String UPDATE_JOB_TOPIC =
            "com/jet2/region-tree/update";

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private JobManager jobManager;

    @Override
    public JobResult process(Job job) {

        LOG.info("Task-19: Region Tree Comparator job started.");

        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, SUBSERVICE);

        try (ResourceResolver resolver =
                     resolverFactory.getServiceResourceResolver(authInfo)) {

            Session session = resolver.adaptTo(Session.class);

            if (session == null) {
                LOG.error("Task-19: Could not obtain JCR session.");
                return JobResult.FAILED;
            }

            // A missing temp tree can never succeed on retry.
            if (!session.nodeExists(TEMP_PATH)) {
                LOG.error("Task-19: Temporary tree does not exist: {}",
                        TEMP_PATH);
                return JobResult.CANCEL;
            }

            Node tempRoot = session.getNode(TEMP_PATH);

            Map<String, Node> tempNodes = new HashMap<>();
            Map<String, Node> actualNodes = new HashMap<>();

            // Read all nodes from the temporary hierarchy.
            RegionTreeUtils.collectNodes(tempRoot, tempNodes);

            // Read actual hierarchy if it exists.
            if (session.nodeExists(ACTUAL_PATH)) {
                RegionTreeUtils.collectNodes(
                        session.getNode(ACTUAL_PATH), actualNodes);
            } else {
                LOG.info("Task-19: Actual tree does not exist. "
                        + "All temporary nodes will be treated as created.");
            }

            List<String> listCreatedNode = new ArrayList<>();
            List<String> listUpdatedNode = new ArrayList<>();
            List<String> listMovedNode = new ArrayList<>();

            RegionTreeUtils.compareNodes(
                    tempNodes,
                    actualNodes,
                    listCreatedNode,
                    listUpdatedNode,
                    listMovedNode);

            LOG.info("Task-19 comparison completed. Created: {}, Updated: {}, Moved: {}",
                    listCreatedNode.size(),
                    listUpdatedNode.size(),
                    listMovedNode.size());

            // Job properties are stored in the repository, so use String[]
            // (a List is not reliably round-tripped).
            Map<String, Object> updateProperties = new HashMap<>();
            updateProperties.put("createdNodes",
                    listCreatedNode.toArray(new String[0]));
            updateProperties.put("updatedNodes",
                    listUpdatedNode.toArray(new String[0]));
            updateProperties.put("movedNodes",
                    listMovedNode.toArray(new String[0]));

            /*
             * Submit the next job before deleting temp, so the comparison
             * output is not lost if submission fails. The next job receives
             * complete JSON data and does not need temp.
             */
            Job updateJob = jobManager.addJob(UPDATE_JOB_TOPIC, updateProperties);

            if (updateJob == null) {
                LOG.error("Task-19: Failed to submit the update job. "
                        + "Temporary tree has been retained.");
                return JobResult.FAILED;
            }

            // Delete temporary hierarchy after successful job submission.
            tempRoot.remove();
            session.save();

            LOG.info("Task-19: Temporary tree deleted successfully.");
            LOG.info("Task-19: Update job submitted successfully. Job ID: {}",
                    updateJob.getId());
            LOG.info("Task-19: Comparator job completed successfully.");

            return JobResult.OK;

        } catch (LoginException e) {

            LOG.error("Task-19: Service resource resolver login failed.", e);
            return JobResult.FAILED;

        } catch (IllegalStateException e) {

            // Duplicate IDs in the tree: retrying will not help.
            LOG.error("Task-19: Invalid tree data, job cancelled.", e);
            return JobResult.CANCEL;

        } catch (Exception e) {

            LOG.error("Task-19: Error while comparing region trees.", e);
            return JobResult.FAILED;
        }
    }
}