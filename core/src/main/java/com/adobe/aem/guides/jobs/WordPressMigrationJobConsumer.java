
package com.adobe.aem.guides.jobs;

import com.adobe.aem.guides.demo.core.models.WordPressArticle;
import com.adobe.aem.guides.demo.core.models.WordPressPage;
import com.adobe.aem.guides.demo.core.services.WordPressMigrationApiService;
import com.adobe.aem.guides.demo.core.services.WordPressContentFragmentService;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;

import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.consumer.JobConsumer;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(
        service = JobConsumer.class,
        property = {
                JobConsumer.PROPERTY_TOPICS
                        + "=com/aem/wordpress/migration"
        }
)
public class WordPressMigrationJobConsumer
        implements JobConsumer {

    private static final Logger LOG =
            LoggerFactory.getLogger(
                    WordPressMigrationJobConsumer.class);

    private static final String SUBSERVICE =
            "wordpress-migration";

    private static final String DESTINATION_PATH =
            "/content/dam/demo/wordpress-migration";

    private static final String JOB_TOPIC =
            "com/aem/wordpress/migration";

    private static final int MAX_PER_PAGE = 100;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private WordPressMigrationApiService apiService;

    @Reference
    private WordPressContentFragmentService contentFragmentService;

    @Reference
    private JobManager jobManager;

    @Override
    public JobResult process(Job job) {

        // 1. Read migration job properties

        String migrationId =
                job.getProperty("migrationId", String.class);

        Integer page =
                job.getProperty("page", Integer.class);

        Integer perPage =
                job.getProperty("perPage", Integer.class);

        Integer maxPages =
                job.getProperty("maxPages", Integer.class);

        // 2. Validate job properties

        if (migrationId == null ||
                page == null ||
                perPage == null ||
                maxPages == null) {

            LOG.error(
                    "Migration job has missing properties.");

            return JobResult.CANCEL;
        }

        if (page < 1 ||
                perPage < 1 ||
                perPage > MAX_PER_PAGE ||
                maxPages < 0) {

            LOG.error(
                    "Invalid migration properties. "
                            + "Migration ID: {}, Page: {}, "
                            + "Per Page: {}, Max Pages: {}",
                    migrationId,
                    page,
                    perPage,
                    maxPages);

            return JobResult.CANCEL;
        }

        LOG.info(
                "Starting WordPress migration. "
                        + "Migration ID: {}, Page: {}, "
                        + "Per Page: {}, Max Pages: {}",
                migrationId,
                page,
                perPage,
                maxPages);

        // 3. Prepare service user authentication

        Map<String, Object> authInfo = new HashMap<>();

        authInfo.put(
                ResourceResolverFactory.SUBSERVICE,
                SUBSERVICE);

        int createdCount = 0;
        int updatedCount = 0;

        try (
                ResourceResolver resolver =
                        resolverFactory.getServiceResourceResolver(authInfo)
        ) {

            // 4. Fetch one page from WordPress

            WordPressPage wordpressPage =
                    apiService.fetchArticles(page, perPage);

            if (wordpressPage == null) {

                throw new IllegalStateException(
                        "WordPress API returned null page");
            }

            List<WordPressArticle> articles =
                    wordpressPage.getArticles();

            if (articles == null) {

                throw new IllegalStateException(
                        "WordPress API returned null article list");
            }

            int totalPages =
                    wordpressPage.getTotalPages();

            int totalPosts =
                    wordpressPage.getTotalPosts();

            // 5. Validate WordPress pagination

            if (totalPages < 0 || totalPosts < 0) {

                throw new IllegalStateException(
                        "Invalid pagination information from WordPress");
            }

            if (totalPosts > 0 && totalPages < 1) {

                throw new IllegalStateException(
                        "WordPress returned invalid total page count");
            }

            if (page > totalPages && totalPosts > 0) {

                throw new IllegalStateException(
                        "Requested page exceeds total WordPress pages");
            }

            if (articles.isEmpty() &&
                    page <= totalPages &&
                    totalPosts > 0) {

                throw new IllegalStateException(
                        "WordPress returned no articles for page "
                                + page
                                + " although total posts exist");
            }

            LOG.info(
                    "Fetched WordPress page {}. "
                            + "Articles: {}, Total Posts: {}, "
                            + "Total Pages: {}",
                    page,
                    articles.size(),
                    totalPosts,
                    totalPages);

            // 6. Process every article in the current page

            for (WordPressArticle article : articles) {

                if (article == null ||
                        article.getWordpressId() == null ||
                        article.getWordpressId().trim().isEmpty()) {

                    throw new IllegalStateException(
                            "Invalid WordPress article found on page "
                                    + page);
                }

                String wordpressId =
                        article.getWordpressId();

                String fragmentName =
                        "wordpress-article-" + wordpressId;

                String fragmentPath =
                        DESTINATION_PATH + "/" + fragmentName;

                // Check whether the fragment already exists

                Resource existing =
                        resolver.getResource(fragmentPath);

                // Create or update the Content Fragment

                String createdOrUpdatedPath =
                        contentFragmentService.createContentFragment(
                                resolver,
                                article);

                if (createdOrUpdatedPath == null ||
                        !fragmentPath.equals(createdOrUpdatedPath)) {

                    throw new IllegalStateException(
                            "Unexpected Content Fragment path for "
                                    + wordpressId);
                }

                if (existing == null) {

                    createdCount++;

                    LOG.info(
                            "Created Content Fragment for WordPress ID: {}",
                            wordpressId);

                } else {

                    updatedCount++;

                    LOG.info(
                            "Updated Content Fragment for WordPress ID: {}",
                            wordpressId);
                }
            }

            // 7. Log current page summary

            LOG.info(
                    "Page {} processing completed. "
                            + "Migration ID: {}, Created: {}, Updated: {}",
                    page,
                    migrationId,
                    createdCount,
                    updatedCount);

            // 8. Determine the last page for this migration

            int lastPage = totalPages;

            if (maxPages > 0) {

                lastPage = Math.min(totalPages, maxPages);
            }

            // 9. Schedule next page only after current page succeeds

            if (page < lastPage) {

                Map<String, Object> nextJobProperties =
                        new HashMap<>();

                nextJobProperties.put(
                        "migrationId",
                        migrationId);

                nextJobProperties.put(
                        "page",
                        page + 1);

                nextJobProperties.put(
                        "perPage",
                        perPage);

                nextJobProperties.put(
                        "maxPages",
                        maxPages);

                Job nextJob =
                        jobManager.addJob(
                                JOB_TOPIC,
                                nextJobProperties);

                if (nextJob == null) {

                    throw new IllegalStateException(
                            "Could not schedule next migration page: "
                                    + (page + 1));
                }

                LOG.info(
                        "Next migration page scheduled. "
                                + "Migration ID: {}, Next Page: {}",
                        migrationId,
                        page + 1);

            } else if (maxPages > 0 &&
                    page >= maxPages &&
                    page < totalPages) {

                // Limited testing completed

                LOG.info(
                        "LIMITED MIGRATION TEST COMPLETED. "
                                + "Migration ID: {}, "
                                + "Pages Processed: {}, "
                                + "Total Available Pages: {}",
                        migrationId,
                        page,
                        totalPages);

            } else {

                // Full migration completed

                LOG.info(
                        "WORDPRESS BULK MIGRATION COMPLETED. "
                                + "Migration ID: {}, "
                                + "Total Posts: {}, "
                                + "Total Pages: {}",
                        migrationId,
                        totalPosts,
                        totalPages);
            }

            return JobResult.OK;

        } catch (Exception e) {

            LOG.error(
                    "WordPress migration job failed. "
                            + "Migration ID: {}, Page: {}",
                    migrationId,
                    page,
                    e);

            // Sling queue retry policy determines retry behaviour.
            return JobResult.FAILED;
        }
    }
}
