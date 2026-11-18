package com.adobe.aem.guides.demo.core.models;

import java.util.List;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class Demo {
    
    @ValueMapValue
    @Default(values = "Bharath")
    private String fullname;

    @ValueMapValue 
    private String number;

    @ChildResource
    private List<Multi> multifield;

    @ChildResource 
    private List<ContactDetails> contactMultifield;

   

    public String getFullname() {
        return fullname;
    }

    public String getNumber() {
        return number;
    }

    public List<Multi> getMultifield() {
        return multifield;
    }

    public List<ContactDetails> getContactMultifield() {
        return contactMultifield;
    }
}