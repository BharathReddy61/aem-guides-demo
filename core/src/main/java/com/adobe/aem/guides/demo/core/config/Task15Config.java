
package com.adobe.aem.guides.demo.core.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
    name = "Task 15 - Page Configuration",
    description = "Configuration for updating AEM page properties"
)
public @interface Task15Config {

    @AttributeDefinition(
        name = "Client ID",
        description = "Client identifier"
    )
    String clientId() default "";

    @AttributeDefinition(
        name = "API Token",
        description = "API authentication token"
    )
    String apiToken() default "";

    @AttributeDefinition(
        name = "Page Path",
        description = "AEM page path to update"
    )
    String pagePath() default "/content/demo/us/en";
}
