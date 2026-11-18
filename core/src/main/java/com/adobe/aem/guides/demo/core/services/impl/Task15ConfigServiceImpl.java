
package com.adobe.aem.guides.demo.core.services.impl;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;

import com.adobe.aem.guides.demo.core.config.Task15Config;
import com.adobe.aem.guides.demo.core.services.Task15ConfigService;

@Component(
    service = Task15ConfigService.class,
    immediate = true
)
@Designate(ocd = Task15Config.class)
public class Task15ConfigServiceImpl
        implements Task15ConfigService {

    private String clientId;
    private String apiToken;
    private String pagePath;

    @Activate
    protected void activate(Task15Config config) {

        clientId = config.clientId();
        apiToken = config.apiToken();
        pagePath = config.pagePath();
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public String getApiToken() {
        return apiToken;
    }

    @Override
    public String getPagePath() {
        return pagePath;
    }
}
