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
public class HeaderModel {

    @ValueMapValue
    private String title;

    @ValueMapValue
    private String linkPath;

    @ValueMapValue
    private Boolean showHeader;

    @ChildResource(name = "items")
    private List<HeaderItem> items;

    public String getTitle() {
        return title;
    }

    public String getLinkPath() {
        return linkPath;
    }

    public boolean isShowHeader() {
        return Boolean.TRUE.equals(showHeader);
    }

    public List<HeaderItem> getItems() {
        return items != null ? items : Collections.emptyList();
    }
}