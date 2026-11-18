
package com.adobe.aem.guides.demo.core.services.impl;

import com.adobe.aem.guides.demo.core.models.WordPressArticle;
import com.adobe.aem.guides.demo.core.services.WordPressContentFragmentService;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.FragmentData;
import com.adobe.cq.dam.cfm.FragmentTemplate;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import org.osgi.service.component.annotations.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

@Component(service = WordPressContentFragmentService.class)
public class WordPressContentFragmentServiceImpl
        implements WordPressContentFragmentService {

    private static final Logger LOG =
            LoggerFactory.getLogger(
                    WordPressContentFragmentServiceImpl.class);

    // Actual Content Fragment Model path
    private static final String MODEL_PATH =
            "/conf/demo/settings/dam/cfm/models/wordpress-article-model";

    // Destination DAM folder
    private static final String DESTINATION_PATH =
            "/content/dam/demo/wordpress-migration";

    @Override
    public String createContentFragment(
            ResourceResolver resolver,
            WordPressArticle article) {

        try {

            // 1. Validate inputs
            if (resolver == null || article == null) {
                throw new IllegalArgumentException(
                        "Resolver and article are required");
            }

            if (!resolver.isLive()) {
                throw new IllegalStateException(
                        "ResourceResolver is not live");
            }

            // 2. Validate WordPress ID
            String wordpressId = article.getWordpressId();

            if (wordpressId == null ||
                    wordpressId.trim().isEmpty()) {

                throw new IllegalArgumentException(
                        "WordPress ID is missing");
            }

            // 3. Get destination DAM folder
            Resource destination =
                    resolver.getResource(DESTINATION_PATH);

            if (destination == null) {
                throw new IllegalStateException(
                        "Destination folder not found: "
                                + DESTINATION_PATH);
            }

            // 4. Get Content Fragment Model
            Resource modelResource =
                    resolver.getResource(MODEL_PATH);

            if (modelResource == null) {
                throw new IllegalStateException(
                        "Content Fragment Model not found: "
                                + MODEL_PATH);
            }

            // 5. Adapt model to FragmentTemplate
            FragmentTemplate template =
                    modelResource.adaptTo(FragmentTemplate.class);

            if (template == null) {
                throw new IllegalStateException(
                        "Could not adapt model to FragmentTemplate");
            }

           
// 6. Generate unique fragment name

String fragmentName =
        "wordpress-article-" + wordpressId;

String fragmentPath =
        DESTINATION_PATH + "/" + fragmentName;

// 7. Check whether the Content Fragment already exists
Resource existingResource =
        resolver.getResource(fragmentPath);

ContentFragment fragment;

String fragmentTitle =
        article.getTitle() != null
                ? article.getTitle()
                : "WordPress Article " + wordpressId;

if (existingResource != null) {

    // Existing fragment: adapt it for updating
    fragment = existingResource.adaptTo(ContentFragment.class);

    if (fragment == null) {
        throw new IllegalStateException(
                "Existing resource is not a valid Content Fragment: "
                        + fragmentPath);
    }

    // Update the Content Fragment title
    fragment.setTitle(fragmentTitle);

    LOG.info(
            "Existing Content Fragment found. Updating: {}",
            fragmentPath);

} else {

    // New fragment: create it using the model
    fragment = template.createFragment(
            destination,
            fragmentName,
            fragmentTitle);

    if (fragment == null) {
        throw new IllegalStateException(
                "Content Fragment creation returned null");
    }

    LOG.info(
            "Creating new Content Fragment: {}",
            fragmentPath);
}


            // 9. Populate WordPress ID
            setText(
                    fragment,
                    "wordpressId",
                    article.getWordpressId());

            // 10. Populate Article Title
            // IMPORTANT: Actual model element name is articleTitle,
            // not title.
            setText(
                    fragment,
                    "articleTitle",
                    article.getTitle());

            // 11. Populate Description
            setText(
                    fragment,
                    "description",
                    article.getDescription());

            // 12. Populate Source URL
            setText(
                    fragment,
                    "sourceUrl",
                    article.getSourceUrl());

            // 13. Populate Slug
            setText(
                    fragment,
                    "slug",
                    article.getSlug());

            // 14. Populate Article Content
            ContentElement contentElement =
                    getRequiredElement(fragment, "content");

            String content =
                    article.getContent() != null
                            ? article.getContent()
                            : "";

            contentElement.setContent(
                    content,
                    "text/html");

            // 15. Populate Published Date
            ContentElement dateElement =
                    getRequiredElement(fragment, "publishedDate");

            Calendar publishedDate =
                    convertDate(article.getPublishedDate());

            FragmentData dateData =
                    dateElement.getValue();

            if (dateData == null) {
                throw new IllegalStateException(
                        "Could not obtain publishedDate FragmentData");
            }

            dateData.setValue(publishedDate);

            dateElement.setValue(dateData);

            // 16. Commit repository changes
            resolver.commit();

            LOG.info(
                    "WordPress Content Fragment created successfully: {}",
                    fragmentPath);

            return fragmentPath;

        } catch (Exception e) {

            LOG.error(
                    "Failed to create Content Fragment for WordPress ID: {}",
                    article != null
                            ? article.getWordpressId()
                            : "unknown",
                    e);

            // Revert uncommitted changes
            if (resolver != null &&
                    resolver.isLive() &&
                    resolver.hasChanges()) {

                resolver.revert();
            }

            throw new RuntimeException(
                    "Content Fragment creation failed: "
                            + e.getMessage(),
                    e);
        }
    }

    /**
     * Sets a text value in a Content Fragment element.
     */
    private void setText(
            ContentFragment fragment,
            String elementName,
            String value) throws Exception {

        ContentElement element =
                getRequiredElement(fragment, elementName);

        element.setContent(
                value != null ? value : "",
                "text/plain");
    }

    /**
     * Gets a required Content Fragment element.
     */
    private ContentElement getRequiredElement(
            ContentFragment fragment,
            String elementName) {

        ContentElement element =
                fragment.getElement(elementName);

        if (element == null) {
            throw new IllegalStateException(
                    "Model element not found: " + elementName);
        }

        return element;
    }

    /**
     * Converts the WordPress date into a Calendar value.
     */
    private Calendar convertDate(String dateString)
            throws Exception {

        if (dateString == null ||
                dateString.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Published date is missing");
        }

        SimpleDateFormat format =
                new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss",
                        Locale.ENGLISH);

        format.setLenient(false);

        // WordPress date returned by the tested API
        // has no timezone suffix. Interpret as UTC here.
        format.setTimeZone(
                TimeZone.getTimeZone("UTC"));

        Date date = format.parse(dateString);

        Calendar calendar =
                Calendar.getInstance(
                        TimeZone.getTimeZone("UTC"));

        calendar.setTime(date);

        return calendar;
    }
}
