package com.adobe.aem.guides.demo.core.models;

import java.util.Collections;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(
    adaptables = Resource.class,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class SidebarModel {

    @ValueMapValue
    private String logoPath;

    @ValueMapValue
    private String logoMobileImage;

    @ValueMapValue
    private String logoLink;

    @ValueMapValue
    private boolean enableSwitch;

    @ChildResource(name = "headerLinks")
    private List<SidebarHeaderLink> headerLinks;

    @ChildResource(name = "navigationItems")
    private List<SidebarNavigationItem> navigationItems;

    @ValueMapValue
    private String country;

    public String getLogoPath() {
        return logoPath;
    }

    public String getLogoMobileImage() {
        return logoMobileImage;
    }

    public String getLogoLink() {
        return logoLink;
    }

    public boolean isEnableSwitch() {
        return enableSwitch;
    }

    public List<SidebarHeaderLink> getHeaderLinks() {
        return headerLinks != null
            ? headerLinks
            : Collections.emptyList();
    }

    public List<SidebarNavigationItem> getNavigationItems() {
        return navigationItems != null
            ? navigationItems
            : Collections.emptyList();
    }

    public String getCountry() {
        return country;
    }
}