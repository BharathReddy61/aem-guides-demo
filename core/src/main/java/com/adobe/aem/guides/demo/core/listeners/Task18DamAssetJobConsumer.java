
package com.adobe.aem.guides.demo.core.listeners;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;

import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;

import com.day.cq.dam.api.Asset;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.Replicator;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = JobConsumer.class,
    property = {
        JobConsumer.PROPERTY_TOPICS + "=" +
        Task18DamAssetChangeListener.JOB_TOPIC
    }
)
public class Task18DamAssetJobConsumer
        implements JobConsumer {

    private static final Logger LOG =
            LoggerFactory.getLogger(
                Task18DamAssetJobConsumer.class
            );

    private static final String SUBSERVICE =
            "task18-asset-publisher";

    private static final String DAM_PATH =
            "/content/dam/demo";

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private Replicator replicator;

    @Override
    public JobResult process(Job job) {

        LOG.info(
            "========== TASK-18 JOB CONSUMER STARTED =========="
        );

        LOG.info(
            "Task-18 DEBUG: Job ID: {}",
            job.getId()
        );

        LOG.info(
            "Task-18 DEBUG: Job Topic: {}",
            job.getTopic()
        );

        String assetPath =
                job.getProperty("assetPath", String.class);

        LOG.info(
            "Task-18 DEBUG: Received asset path: [{}]",
            assetPath
        );

        // Validate asset path.
        if (assetPath == null ||
                assetPath.trim().isEmpty()) {

            LOG.error(
                "Task-18 ERROR: Asset path is NULL or EMPTY."
            );

            return JobResult.CANCEL;
        }

        // Validate monitored DAM folder.
        if (!assetPath.startsWith(DAM_PATH + "/")) {

            LOG.error(
                "Task-18 ERROR: Asset path is outside monitored " +
                "DAM folder: {}",
                assetPath
            );

            return JobResult.CANCEL;
        }

        Map<String, Object> authInfo =
                new HashMap<>();

        authInfo.put(
            ResourceResolverFactory.SUBSERVICE,
            SUBSERVICE
        );

        try (ResourceResolver resolver =
                resolverFactory.getServiceResourceResolver(
                    authInfo
                )) {

            LOG.info(
                "Task-18 DEBUG: Service ResourceResolver obtained."
            );

            // Obtain the asset resource.
            Resource resource =
                    resolver.getResource(assetPath);

            if (resource == null) {

                LOG.error(
                    "Task-18 ERROR: Asset resource not found: {}",
                    assetPath
                );

                return JobResult.CANCEL;
            }

            LOG.info(
                "Task-18 DEBUG: Resource found: {}",
                resource.getPath()
            );

            // Verify that the resource is a DAM Asset.
            Asset asset =
                    resource.adaptTo(Asset.class);

            if (asset == null) {

                LOG.error(
                    "Task-18 ERROR: Resource is not a DAM Asset: {}",
                    assetPath
                );

                return JobResult.CANCEL;
            }

            LOG.info(
                "Task-18 DEBUG: DAM Asset validation successful: {}",
                asset.getPath()
            );

            // Obtain JCR Session.
            Session session =
                    resolver.adaptTo(Session.class);

            if (session == null) {

                LOG.error(
                    "Task-18 ERROR: Could not obtain JCR Session."
                );

                return JobResult.FAILED;
            }

            LOG.info(
                "Task-18 DEBUG: JCR Session obtained."
            );

            // Publish asset using Replicator.
            LOG.info(
                "Task-18 DEBUG: Starting asset activation: {}",
                assetPath
            );

            replicator.replicate(
                session,
                ReplicationActionType.ACTIVATE,
                assetPath
            );

            LOG.info(
                "Task-18 SUCCESS: Successfully submitted asset " +
                "for publication: {}",
                assetPath
            );

            LOG.info(
                "========== TASK-18 JOB COMPLETED =========="
            );

            return JobResult.OK;

        } catch (Exception e) {

            LOG.error(
                "Task-18 ERROR: Exception while publishing asset: {}",
                assetPath,
                e
            );

            return JobResult.FAILED;
        }
    }
}
