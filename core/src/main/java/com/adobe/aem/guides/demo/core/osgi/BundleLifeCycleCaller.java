package com.adobe.aem.guides.demo.core.osgi;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = BundleLifeCycleCaller.class, immediate = true)
public class BundleLifeCycleCaller {

    private static final Logger LOG =
            LoggerFactory.getLogger(BundleLifeCycleCaller.class);

    @Reference
    private BundleLifeCycle bundleLifeCycle;

    @Activate
    protected void activate() {

        LOG.info("Calling activate method");
        bundleLifeCycle.executeActivate();

        LOG.info("Calling modified method");
        bundleLifeCycle.executeModified();

        LOG.info("Calling deactivate method");
        bundleLifeCycle.executeDeactivate();
    }
}