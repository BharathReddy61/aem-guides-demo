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
public class CarouselItem {

    @ValueMapValue
    private String text;

    @ValueMapValue
    private String path;

    @ChildResource(name = "nestedItems")
    private List<CarouselNestedItem> nestedItems;

    public String getText() {
        return text;
    }

    public String getPath() {
        return path;
    }

    public List<CarouselNestedItem> getNestedItems() {
        return nestedItems != null
            ? nestedItems
            : Collections.emptyList();
    }
}