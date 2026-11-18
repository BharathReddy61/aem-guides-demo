
package com.adobe.aem.guides.demo.core.models;

import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(
    adaptables = Resource.class,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class RestaurantMenuModel {

    @ValueMapValue
    private String restaurantName;

    @ChildResource(name = "menuItems")
    private List<Resource> menuItemResources;

    private List<MenuItemModel> menuItems;

    @PostConstruct
    protected void init() {
        if (menuItemResources == null || menuItemResources.isEmpty()) {
            menuItems = Collections.emptyList();
            return;
        }

        menuItems = menuItemResources.stream()
            .map(resource -> resource.adaptTo(MenuItemModel.class))
            .filter(item -> item != null)
            .collect(java.util.stream.Collectors.toList());
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public List<MenuItemModel> getMenuItems() {
        return menuItems == null ? Collections.emptyList() : menuItems;
    }
}
