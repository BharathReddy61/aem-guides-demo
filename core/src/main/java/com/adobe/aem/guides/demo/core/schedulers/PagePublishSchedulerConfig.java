package com.adobe.aem.guides.demo.core.schedulers;


import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Demo - Page Publish Scheduler Configuration")
public @interface PagePublishSchedulerConfig {

    @AttributeDefinition(name = "Scheduler Name")
    String schedulerName() default "PagePublishScheduler";

    @AttributeDefinition(name = "Cron Expression",
            description = "Quartz cron, e.g. 0 0/5 * * * ? (every 5 minutes)")
    String cronExpression() default "0 0 0 * * ?";

    @AttributeDefinition(name = "Page Path",
            description = "This page and all its descendants will be published")
    String pagePath() default "/content/demo/us";

    @AttributeDefinition(name = "Enabled")
    boolean enabled() default true;
}
