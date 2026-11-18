package com.adobe.aem.guides.demo.core.osgi;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = BundleLifeCycle.class, immediate = true)
public class BundleLifeCycle {

    private static final Logger LOG =
            LoggerFactory.getLogger(BundleLifeCycle.class);

    @Activate
    protected void activate() {
        LOG.info("MY CMP is activated");
    }

    @Modified
    protected void modified() {
        LOG.info("MY CMP is modified");
    }

    @Deactivate
    protected void deactivate() {
        LOG.info("MY CMP is deactivated");
    }

    // Public methods that another component can call
    public void executeActivate() {
        activate();
    }

    public void executeModified() {
        modified();
    }

    public void executeDeactivate() {
        deactivate();
    }
}