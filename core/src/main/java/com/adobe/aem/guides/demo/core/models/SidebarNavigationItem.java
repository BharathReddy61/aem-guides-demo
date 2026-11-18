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
public class SidebarNavigationItem {

    @ValueMapValue
    private String desktopIcon;

    @ValueMapValue
    private String mobileIcon;

    @ChildResource(name = "nestedNavigation")
    private List<SidebarNestedNavigation> nestedNavigation;

    public String getDesktopIcon() {
        return desktopIcon;
    }

    public String getMobileIcon() {
        return mobileIcon;
    }

    public List<SidebarNestedNavigation> getNestedNavigation() {
        return nestedNavigation != null
            ? nestedNavigation
            : Collections.emptyList();
    }
}