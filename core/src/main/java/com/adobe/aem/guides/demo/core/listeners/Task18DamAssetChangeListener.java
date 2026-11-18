
package com.adobe.aem.guides.demo.core.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = ResourceChangeListener.class,
    property = {
        ResourceChangeListener.PATHS + "=/content/dam/demo",
        ResourceChangeListener.CHANGES + "=CHANGED"
    }
)
public class Task18DamAssetChangeListener
        implements ResourceChangeListener {

    private static final Logger LOG =
            LoggerFactory.getLogger(
                Task18DamAssetChangeListener.class
            );

    private static final String DAM_PATH =
            "/content/dam/demo";

    public static final String JOB_TOPIC =
            "com/adobe/aem/guides/demo/task18/publish-asset";

    @Reference
    private JobManager jobManager;

    @Override
    public void onChange(List<ResourceChange> changes) {

        LOG.info(
            "========== TASK-18 LISTENER STARTED =========="
        );

        LOG.info(
            "Task-18 DEBUG: Total resource changes received: {}",
            changes.size()
        );

        // Prevent duplicate jobs for the same asset
        // within one notification batch.
        Set<String> processedAssets = new HashSet<>();

        for (ResourceChange change : changes) {

            String changedPath = change.getPath();

            LOG.info(
                "Task-18 DEBUG: Change type: {}, Changed path: {}",
                change.getType(),
                changedPath
            );

            if (changedPath == null || changedPath.trim().isEmpty()) {

                LOG.warn(
                    "Task-18: Empty resource change path. Skipping."
                );

                continue;
            }

            // Process only resources inside the configured DAM folder.
            if (!changedPath.startsWith(DAM_PATH + "/")) {

                LOG.debug(
                    "Task-18: Path outside monitored DAM folder: {}",
                    changedPath
                );

                continue;
            }

            // Convert a child resource path into its asset path.
            String assetPath = changedPath;

            int contentIndex =
                    assetPath.indexOf("/jcr:content");

            if (contentIndex != -1) {

                assetPath =
                        assetPath.substring(0, contentIndex);
            }

            LOG.info(
                "Task-18 DEBUG: Normalized asset path: {}",
                assetPath
            );

            // Avoid duplicate jobs in the same batch.
            if (!processedAssets.add(assetPath)) {

                LOG.info(
                    "Task-18: Duplicate change ignored for: {}",
                    assetPath
                );

                continue;
            }

            Map<String, Object> jobProperties =
                    new HashMap<>();

            jobProperties.put("assetPath", assetPath);

            LOG.info(
                "Task-18 DEBUG: Creating Sling Job for: {}",
                assetPath
            );

            try {

                Job job = jobManager.addJob(
                    JOB_TOPIC,
                    jobProperties
                );

                if (job != null) {

                    LOG.info(
                        "Task-18: Sling Job created successfully. " +
                        "Job ID: {}, Asset: {}",
                        job.getId(),
                        assetPath
                    );

                } else {

                    LOG.error(
                        "Task-18: Sling Job creation returned null " +
                        "for asset: {}",
                        assetPath
                    );
                }

            } catch (Exception e) {

                LOG.error(
                    "Task-18: Exception while creating Sling Job " +
                    "for asset: {}",
                    assetPath,
                    e
                );
            }
        }

        LOG.info(
            "========== TASK-18 LISTENER COMPLETED =========="
        );
    }
}
