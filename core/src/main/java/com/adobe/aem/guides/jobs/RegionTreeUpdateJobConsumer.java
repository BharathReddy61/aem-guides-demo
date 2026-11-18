package com.adobe.aem.guides.jobs;

import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Region Tree Update job.
 *
 * Topic: com/jet2/region-tree/update
 *
 * For now it only logs the created / updated / moved nodes.
 * The actual tree is NOT modified (out of scope for Task-19).
 */
@Component(
    service = JobConsumer.class,
    property = {
        JobConsumer.PROPERTY_TOPICS + "=com/jet2/region-tree/update"
    }
)
public class RegionTreeUpdateJobConsumer implements JobConsumer {

    private static final Logger LOG =
            LoggerFactory.getLogger(RegionTreeUpdateJobConsumer.class);

    @Override
    public JobResult process(Job job) {

        LOG.info("Task-19: Region Tree Update job started.");

        try {
            String[] createdNodes =
                    job.getProperty("createdNodes", String[].class);

            String[] updatedNodes =
                    job.getProperty("updatedNodes", String[].class);

            String[] movedNodes =
                    job.getProperty("movedNodes", String[].class);

            LOG.info("========== TASK-19 UPDATE JOB ==========");

            logNodes("CREATED NODES", createdNodes);
            logNodes("UPDATED NODES", updatedNodes);
            logNodes("MOVED NODES", movedNodes);

            LOG.info("Task-19: Update job completed.");
            LOG.info("Actual region tree was not modified.");

            return JobResult.OK;

        } catch (Exception e) {

            LOG.error("Task-19: Error processing update job.", e);

            return JobResult.FAILED;
        }
    }

    private void logNodes(String category, String[] nodes) {

        LOG.info("---------- {} ----------", category);

        if (nodes == null || nodes.length == 0) {
            LOG.info("No nodes found.");
            return;
        }

        for (String node : nodes) {
            LOG.info("{}", node);
        }

        LOG.info("Total {}: {}", category, nodes.length);
    }
}