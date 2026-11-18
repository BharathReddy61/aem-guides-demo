package com.adobe.aem.guides.demo.core.schedulers;

import java.util.*;
import javax.jcr.Session;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageFilter;
import com.day.cq.wcm.api.PageManager;

@Component(service = Runnable.class, immediate = true)
@Designate(ocd = PagePublishSchedulerConfig.class)
public class PagePublishScheduler implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(PagePublishScheduler.class);
    private static final String SUBSERVICE = "demo-publish-service";

    @Reference
    private Scheduler scheduler;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private Replicator replicator;

    private String schedulerName;
    private String pagePath;

    @Activate
    @Modified
    protected void activate(PagePublishSchedulerConfig config) {
        if (schedulerName != null) {
            scheduler.unschedule(schedulerName);
        }
        schedulerName = config.schedulerName();
        pagePath = config.pagePath();

        if (config.enabled()) {
            ScheduleOptions options = scheduler.EXPR(config.cronExpression());
            options.name(schedulerName);
            options.canRunConcurrently(false);
            options.onLeaderOnly(true);
            scheduler.schedule(this, options);
            LOG.info("Scheduler '{}' registered. Cron: [{}], Page path: [{}]",
                    schedulerName, config.cronExpression(), pagePath);
        } else {
            LOG.info("Scheduler '{}' is disabled", schedulerName);
        }
    }

    @Deactivate
    protected void deactivate() {
        if (schedulerName != null) {
            scheduler.unschedule(schedulerName);
            LOG.info("Scheduler '{}' unregistered", schedulerName);
        }
    }
    @Override
public void run() {
    LOG.info("===== Scheduler job started =====");
    LOG.info("Parent page path: {}", pagePath);

    Map<String, Object> authInfo =
            Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, SUBSERVICE);

    try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
        PageManager pageManager = resolver.adaptTo(PageManager.class);
        Session session = resolver.adaptTo(Session.class);
        Page parentPage = pageManager.getPage(pagePath);

        if (parentPage == null) {
            LOG.warn("No page found at parent path: {}", pagePath);
            return;
        }

        // Collect only the descendants (children, grandchildren, ...)
        List<String> descendants = new ArrayList<>();
        Iterator<Page> it = parentPage.listChildren(new PageFilter(), true);
        while (it.hasNext()) {
            descendants.add(it.next().getPath());
        }

        LOG.info("Found {} descendant page(s) under parent path: {}", descendants.size(), pagePath);

        int success = 0, failed = 0;
        for (String path : descendants) {
            try {
                replicator.replicate(session, ReplicationActionType.ACTIVATE, path);
                LOG.info("Published descendant: {}", path);
                success++;
            } catch (ReplicationException e) {
                LOG.error("Failed to publish descendant: {}", path, e);
                failed++;
            }
        }

        LOG.info("Parent: {} | Total: {} | Published: {} | Failed: {}",
                pagePath, descendants.size(), success, failed);
        LOG.info("===== Scheduler job finished =====");

    } catch (LoginException e) {
        LOG.error("Could not get service resolver for sub-service '{}'", SUBSERVICE, e);
    } catch (Exception e) {
        LOG.error("Unexpected error in page publish job", e);
    }
}
}
