
package com.adobe.aem.guides.demo.core.listeners;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Session;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.PersistenceException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;

@Component(
    service = EventHandler.class,
    property = {
        EventConstants.EVENT_TOPIC + "=" + ReplicationAction.EVENT_TOPIC
    }
)
public class Task7ReplicationEventHandler implements EventHandler {

    private static final Logger LOG =
            LoggerFactory.getLogger(Task7ReplicationEventHandler.class);

    private static final String SUBSERVICE_NAME = "task7-page-writer";

    private static final String CONTENT_ROOT = "/content/demo";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    public void handleEvent(Event event) {

        ReplicationAction action = ReplicationAction.fromEvent(event);

        if (action == null) {
            LOG.warn("Task-7: Unable to read replication action.");
            return;
        }

        // Process only activation events.
        if (action.getType() != ReplicationActionType.ACTIVATE) {
            return;
        }

        String pagePath = action.getPath();

        if (pagePath == null || pagePath.trim().isEmpty()) {
            LOG.warn("Task-7: Empty replication path.");
            return;
        }

        // Ignore Experience Fragments and content outside this project.
        if (!pagePath.startsWith(CONTENT_ROOT + "/")) {
            LOG.info("Task-7: Ignoring non-demo content: {}", pagePath);
            return;
        }

        LOG.info("Task-7: Page activation event received for {}", pagePath);

        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(
                ResourceResolverFactory.SUBSERVICE,
                SUBSERVICE_NAME
        );

        try (ResourceResolver serviceResolver =
                resourceResolverFactory.getServiceResourceResolver(authInfo)) {

            LOG.info("========== TASK-7 SERVICE USER DIAGNOSTIC ==========");

            // 1. Verify actual service resolver identity.
            LOG.info("Expected service user: {}", SUBSERVICE_NAME);
            LOG.info("Actual service user: {}", serviceResolver.getUserID());

            LOG.info(
                "Correct service identity: {}",
                SUBSERVICE_NAME.equals(serviceResolver.getUserID())
            );

            // 2. Check effective JCR permissions.
            Session session = serviceResolver.adaptTo(Session.class);

            if (session == null) {
                LOG.error("Task-7: Cannot adapt service resolver to JCR Session.");
                return;
            }

            String contentPath = pagePath + "/jcr:content";

            String[] paths = {
                "/",
                "/content",
                CONTENT_ROOT,
                pagePath,
                contentPath
            };

            LOG.info("========== TASK-7 JCR PERMISSION CHECK ==========");

            for (String path : paths) {

                boolean canRead =
                        session.hasPermission(path, Session.ACTION_READ);

                boolean canSetProperty =
                        session.hasPermission(
                                path,
                                Session.ACTION_SET_PROPERTY
                        );

                Resource resource = serviceResolver.getResource(path);

                LOG.info(
                    "Path: {} | Resource exists: {} | Read: {} | Set Property: {}",
                    path,
                    resource != null,
                    canRead,
                    canSetProperty
                );
            }

            LOG.info("==================================================");

            // 3. Verify the page resource.
            Resource pageResource =
                    serviceResolver.getResource(pagePath);

            if (pageResource == null) {
                LOG.error(
                    "Task-7: Page cannot be accessed: {}",
                    pagePath
                );
                return;
            }

            // 4. Verify the page jcr:content resource.
            Resource contentResource =
                    serviceResolver.getResource(contentPath);

            if (contentResource == null) {
                LOG.error(
                    "Task-7: Page content resource cannot be accessed: {}",
                    contentPath
                );
                return;
            }

            // 5. Adapt to ModifiableValueMap.
            ModifiableValueMap properties =
                    contentResource.adaptTo(ModifiableValueMap.class);

            if (properties == null) {
                LOG.error(
                    "Task-7: Cannot adapt content resource to ModifiableValueMap: {}",
                    contentPath
                );
                return;
            }

            // 6. Update the changed property.
            properties.put("changed", Boolean.TRUE);

            // 7. Persist the change.
            serviceResolver.commit();

            LOG.info(
                "Task-7: Successfully set changed=true at {}",
                contentPath
            );

            LOG.info("====================================================");

        } catch (org.apache.sling.api.resource.LoginException e) {

            LOG.error(
                "Task-7: Service resolver login failed for subservice {}.",
                SUBSERVICE_NAME,
                e
            );

        } catch (PersistenceException e) {

            LOG.error(
                "Task-7: Failed to save changed property for {}",
                pagePath,
                e
            );

        } catch (Exception e) {

            LOG.error(
                "Task-7: Unexpected error while processing {}",
                pagePath,
                e
            );
        }
    }
}
