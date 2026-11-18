package com.adobe.aem.guides.demo.core.utils;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for Region Tree synchronization.
 *
 * 1. Building the temporary region tree (used by the servlet).
 * 2. Traversing a tree and collecting nodes by key (used by the job).
 * 3. Comparing temp and actual trees (used by the job).
 */
public final class RegionTreeUtils {

    private static final Logger LOG =
            LoggerFactory.getLogger(RegionTreeUtils.class);

    private static final String NODE_TYPE = "nt:unstructured";

    private RegionTreeUtils() {
        // Utility class.
    }

    // =====================================================================
    // BUILD (servlet)
    // =====================================================================

    /**
     * Builds the complete region tree under the supplied temp node.
     */
    public static void buildTempTree(Node tempRoot, JSONObject payload)
            throws RepositoryException, JSONException {

        JSONArray regions = payload.optJSONArray("regions");

        if (regions == null) {
            throw new JSONException(
                    "Invalid JSON: regions array is missing.");
        }

        LOG.info("Region Tree: Starting temporary tree creation.");

        for (int i = 0; i < regions.length(); i++) {

            JSONObject regionJson = regions.getJSONObject(i);
            String regionId = getRequiredId(regionJson);

            Node regionNode = tempRoot.addNode(regionId, NODE_TYPE);

            populateProperties(regionNode, regionJson,
                    "region", regionId, null);

            LOG.info("Region Tree: Region created: {}", regionNode.getPath());

            JSONArray areas = regionJson.optJSONArray("areas");

            if (areas == null) {
                continue;
            }

            for (int j = 0; j < areas.length(); j++) {

                JSONObject areaJson = areas.getJSONObject(j);
                String areaId = getRequiredId(areaJson);

                Node areaNode = regionNode.addNode(areaId, NODE_TYPE);

                populateProperties(areaNode, areaJson,
                        "area", regionId, areaId);

                LOG.info("Region Tree: Area created: {}", areaNode.getPath());

                JSONArray resorts = areaJson.optJSONArray("resorts");

                if (resorts == null) {
                    continue;
                }

                for (int k = 0; k < resorts.length(); k++) {

                    JSONObject resortJson = resorts.getJSONObject(k);
                    String resortId = getRequiredId(resortJson);

                    Node resortNode = areaNode.addNode(resortId, NODE_TYPE);

                    populateProperties(resortNode, resortJson,
                            "resort", regionId, areaId);

                    LOG.info("Region Tree: Resort created: {}",
                            resortNode.getPath());
                }
            }
        }

        LOG.info("Region Tree: Temporary hierarchy creation completed.");
    }

    /**
     * Populates properties on region, area and resort nodes.
     */
    private static void populateProperties(
            Node node,
            JSONObject source,
            String nodeType,
            String regionId,
            String areaId)
            throws RepositoryException, JSONException {

        String id = getRequiredId(source);
        String name = getOptionalString(source, "name");
        String title = getOptionalString(source, "title");

        // Use name as title when title is not supplied.
        if (title == null || title.trim().isEmpty()) {
            title = name;
        }

        node.setProperty("id", id);
        node.setProperty("nodeType", nodeType);

        if (name != null) {
            node.setProperty("name", name);
        }

        if (title != null) {
            node.setProperty("title", title);
        }

        if (regionId != null) {
            node.setProperty("regionId", regionId);
        }

        if (areaId != null) {
            node.setProperty("areaId", areaId);
        }

        setOptionalProperty(node, source, "damPath");
        setOptionalProperty(node, source, "tagPath");

        String lastModified = getOptionalString(source, "lastModified");

        if (lastModified == null || lastModified.trim().isEmpty()) {
            lastModified = Instant.now().toString();
        }

        node.setProperty("lastModified", lastModified);

        LOG.debug("Region Tree: Properties populated for {} node: {}",
                nodeType, node.getPath());
    }

    // =====================================================================
    // TRAVERSAL (job consumer)
    // =====================================================================

    /**
     * Recursively collects every node having an "id" property.
     *
     * The map key is "nodeType:id" (for example "resort:180"), so a region,
     * an area and a resort may safely share the same numeric ID.
     *
     * @throws IllegalStateException if the same key appears twice.
     */
    public static void collectNodes(Node parent, Map<String, Node> nodeMap)
            throws RepositoryException {

        NodeIterator children = parent.getNodes();

        while (children.hasNext()) {

            Node child = children.nextNode();

            if (child.hasProperty("id")) {

                String key = buildKey(child);

                if (nodeMap.containsKey(key)) {
                    throw new IllegalStateException(
                            "Duplicate node found: " + key);
                }

                nodeMap.put(key, child);
            }

            collectNodes(child, nodeMap);
        }
    }

    /**
     * Builds the comparison key "nodeType:id" for a node.
     */
    public static String buildKey(Node node) throws RepositoryException {

        String type = getPropertyValue(node, "nodeType");

        if (type.isEmpty()) {
            type = "unknown";
        }

        return type + ":" + getPropertyValue(node, "id");
    }

    // =====================================================================
    // COMPARISON (job consumer)
    // =====================================================================

    /**
     * Compares temp nodes with actual nodes.
     *
     * Created: key does not exist in actual.
     * Updated: key exists, but title changed.
     * Moved:   key exists, but regionId or areaId changed.
     *
     * A node whose title and parent both changed appears in both the
     * Updated and the Moved list.
     */
    public static void compareNodes(
            Map<String, Node> tempNodes,
            Map<String, Node> actualNodes,
            List<String> listCreatedNode,
            List<String> listUpdatedNode,
            List<String> listMovedNode)
            throws RepositoryException, JSONException {

        for (Map.Entry<String, Node> entry : tempNodes.entrySet()) {

            String key = entry.getKey();
            Node tempNode = entry.getValue();
            Node actualNode = actualNodes.get(key);

            if (actualNode == null) {
                listCreatedNode.add(convertNodeToJson(tempNode));
                LOG.info("Region Tree: Created node detected: {}", key);
                continue;
            }

            boolean titleChanged = !getPropertyValue(tempNode, "title")
                    .equals(getPropertyValue(actualNode, "title"));

            boolean regionChanged = !getPropertyValue(tempNode, "regionId")
                    .equals(getPropertyValue(actualNode, "regionId"));

            boolean areaChanged = !getPropertyValue(tempNode, "areaId")
                    .equals(getPropertyValue(actualNode, "areaId"));

            if (titleChanged) {
                listUpdatedNode.add(convertNodeToJson(tempNode));
                LOG.info("Region Tree: Updated node detected: {}", key);
            }

            if (regionChanged || areaChanged) {
                listMovedNode.add(convertNodeToJson(tempNode));
                LOG.info("Region Tree: Moved node detected: {}", key);
            }
        }
    }

    /**
     * Reads a JCR property as a String.
     * Returns an empty String when the property is absent.
     */
    public static String getPropertyValue(Node node, String propertyName)
            throws RepositoryException {

        if (node.hasProperty(propertyName)) {
            return node.getProperty(propertyName).getString();
        }

        return "";
    }

    /**
     * Converts a JCR node into a JSON string, so the next job can use the
     * data after the temp tree has been deleted.
     */
    public static String convertNodeToJson(Node node)
            throws RepositoryException, JSONException {

        JSONObject json = new JSONObject();

        putProperty(json, node, "id");
        putProperty(json, node, "nodeType");
        putProperty(json, node, "name");
        putProperty(json, node, "title");
        putProperty(json, node, "regionId");
        putProperty(json, node, "areaId");
        putProperty(json, node, "damPath");
        putProperty(json, node, "tagPath");
        putProperty(json, node, "lastModified");

        json.put("path", node.getPath());

        return json.toString();
    }

    private static void putProperty(
            JSONObject json,
            Node node,
            String propertyName)
            throws RepositoryException, JSONException {

        if (node.hasProperty(propertyName)) {
            json.put(propertyName,
                    node.getProperty(propertyName).getString());
        }
    }

    // =====================================================================
    // JSON helpers
    // =====================================================================

    /**
     * Reads a mandatory ID from JSON (string or numeric).
     */
    private static String getRequiredId(JSONObject json)
            throws JSONException {

        String id = getOptionalString(json, "id");

        if (id == null || id.trim().isEmpty()) {
            throw new JSONException(
                    "Missing mandatory ID in region tree JSON.");
        }

        return id.trim();
    }

    /**
     * Reads a JSON value and converts it to String.
     */
    private static String getOptionalString(
            JSONObject json,
            String propertyName) {

        Object value = json.opt(propertyName);

        if (value == null || value == JSONObject.NULL) {
            return null;
        }

        return String.valueOf(value);
    }

    /**
     * Stores optional properties only when supplied.
     */
    private static void setOptionalProperty(
            Node node,
            JSONObject source,
            String propertyName)
            throws RepositoryException {

        String value = getOptionalString(source, propertyName);

        if (value != null) {
            node.setProperty(propertyName, value);
        }
    }
}