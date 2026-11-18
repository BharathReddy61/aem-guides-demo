package com.adobe.aem.guides.demo.core.models;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(
    adaptables = Resource.class,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class HeaderItem {

    @ValueMapValue
    private String itemText;

    @ValueMapValue
    private Calendar itemDate;

    public String getItemText() {
        return itemText;
    }

    public Calendar getItemDate() {
        return itemDate;
    }

    public String getFormattedDate() {
        if (itemDate == null) {
            return "";
        }

        SimpleDateFormat formatter =
            new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

        return formatter.format(itemDate.getTime());
    }
}