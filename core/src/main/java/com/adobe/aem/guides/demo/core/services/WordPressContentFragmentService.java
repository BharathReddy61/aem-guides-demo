
package com.adobe.aem.guides.demo.core.services;

import com.adobe.aem.guides.demo.core.models.WordPressArticle;
import org.apache.sling.api.resource.ResourceResolver;

public interface WordPressContentFragmentService {

    String createContentFragment(
            ResourceResolver resolver,
            WordPressArticle article);
}
