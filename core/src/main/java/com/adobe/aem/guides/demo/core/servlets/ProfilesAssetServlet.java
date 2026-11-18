
package com.adobe.aem.guides.demo.core.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;

import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.cq.dam.api.Asset;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.resourceTypes=demo/components/profiles/datasource",
        "sling.servlet.methods=GET"
    }
)
public class ProfilesAssetServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final String DAM_FOLDER =
        "/content/dam/images";

    @Override
    protected void doGet(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response)
            throws ServletException, IOException {

        ResourceResolver resolver = request.getResourceResolver();

        List<Resource> options = new ArrayList<>();

        Resource folder = resolver.getResource(DAM_FOLDER);

        if (folder != null) {

            Iterator<Resource> children = folder.listChildren();

            while (children.hasNext()) {

                Resource child = children.next();

                Asset asset = child.adaptTo(Asset.class);

                if (asset == null) {
                    continue;
                }

                String assetName = asset.getName();
                String assetPath = asset.getPath();

                Map<String, Object> values = new HashMap<>();

                // Text displayed in the dropdown
                values.put("text", assetName);

                // Value saved in component properties
                values.put("value", assetPath);

                ValueMap valueMap =
                    new ValueMapDecorator(values);

                Resource option = new ValueMapResource(
                    resolver,
                    child.getPath() + "/dropdown-option",
                    "nt:unstructured",
                    valueMap
                );

                options.add(option);
            }
        }

        // Display filenames in alphabetical order
        options.sort(
            Comparator.comparing(
                option -> option.getValueMap().get(
                    "text", String.class
                ),
                String.CASE_INSENSITIVE_ORDER
            )
        );

        DataSource dataSource =
            new SimpleDataSource(options.iterator());

        request.setAttribute(
            DataSource.class.getName(),
            dataSource
        );
    }
}
