package com.adobe.aem.guides.demo.core.schedulers;

import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.aem.guides.demo.core.services.Task15PageUpdateService;

@Component(
    service = Task15PageUpdateScheduler.class,
    immediate = true
)
public class Task15PageUpdateScheduler {

    private static final Logger LOG =
        LoggerFactory.getLogger(Task15PageUpdateScheduler.class);

    private static final String JOB_NAME =
        "Task15PageUpdateScheduler";

    private static final String CRON_EXPRESSION =
        "0 0/5 * * * ?";

    @Reference
    private Scheduler scheduler;

    @Reference
    private Task15PageUpdateService pageUpdateService;

    @Activate
    protected void activate() {

        LOG.info("Task-15: Registering scheduler.");

        try {

            ScheduleOptions options =
                scheduler.EXPR(CRON_EXPRESSION);

            options.name(JOB_NAME);
            options.canRunConcurrently(false);

            boolean scheduled =
                scheduler.schedule(new Runnable() {
        @Override
        public void run() {
            executeTask();
        }
    }, options);

            if (scheduled) {
                LOG.info("Task-15: Scheduler registered successfully.");
            } else {
                LOG.error("Task-15: Scheduler registration returned false.");
            }

        } catch (Exception e) {

            LOG.error("Task-15: Unable to register scheduler.", e);
        }
    }

    private void executeTask() {

        LOG.info("========== TASK-15 SCHEDULER STARTED ==========");

        try {

            String result =
                pageUpdateService.updatePageProperties();

            LOG.info("Task-15 Scheduler Result: {}", result);

        } catch (Exception e) {

            LOG.error("Task-15: Scheduler execution failed.", e);
        }

        LOG.info("========== TASK-15 SCHEDULER COMPLETED ==========");
    }

    @Deactivate
    protected void deactivate() {

        scheduler.unschedule(JOB_NAME);

        LOG.info("Task-15: Scheduler unregistered.");
    }
}