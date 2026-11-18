package com.adobe.aem.guides.demo.core.servlets;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.paths=/bin/demo/contact",
        "sling.servlet.methods=GET",
        "sling.servlet.methods=POST"
    }
)
public class ContactServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG =
        LoggerFactory.getLogger(ContactServlet.class);

    private static final String CONTACTS_PATH = "/var/demo/contacts";
    private static final String SUBSERVICE = "demo-contact-service";

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private static final Pattern MOBILE_PATTERN =
        Pattern.compile("^[+]?[0-9()\\-\\s]{7,20}$");

    private static final Pattern UUID_PATTERN =
        Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-" +
            "[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-" +
            "[0-9a-fA-F]{12}$");

    @Reference
    private ResourceResolverFactory resolverFactory;


    // =====================================================
    // GET: READ ALL CONTACTS, or ONE CONTACT when ?id=<uuid>
    // =====================================================

    @Override
    protected void doGet(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response)
            throws ServletException, IOException {

        prepareResponse(response);

        ResourceResolver resolver = null;

        try {
            resolver = getServiceResolver();
            Session session = getSession(resolver);

            if (!session.nodeExists(CONTACTS_PATH)) {
                sendError(response, 500,
                    "Contact repository is unavailable.");
                return;
            }

            String id = clean(request.getParameter("id"));

            // ---- single contact ----
            if (!id.isEmpty()) {

                if (!validateId(id, response)) {
                    return;
                }

                String contactPath = CONTACTS_PATH + "/contact-" + id;

                if (!session.nodeExists(contactPath)) {
                    sendError(response, 404, "Contact not found.");
                    return;
                }

                Node contact = session.getNode(contactPath);

                if (!hasContactProperties(contact)) {
                    sendError(response, 404, "Contact not found.");
                    return;
                }

                response.setStatus(200);
                response.getWriter().write(contactToJson(contact));
                return;
            }

            // ---- all contacts ----
            Node root = session.getNode(CONTACTS_PATH);
            NodeIterator children = root.getNodes();

            StringBuilder json = new StringBuilder("[");
            boolean first = true;

            while (children.hasNext()) {

                Node contact = children.nextNode();

                if (!hasContactProperties(contact)) {
                    continue;
                }

                if (!first) {
                    json.append(",");
                }

                json.append(contactToJson(contact));
                first = false;
            }

            json.append("]");

            response.setStatus(200);
            response.getWriter().write(json.toString());

            LOG.info("TASK-9: Contacts retrieved successfully.");

        } catch (LoginException e) {

            LOG.error("TASK-9: Service resolver login failed.", e);
            sendError(response, 500,
                "Unable to obtain contact service resolver.");

        } catch (RepositoryException e) {

            LOG.error("TASK-9: Failed to retrieve contacts.", e);
            sendError(response, 500,
                "Unable to retrieve contact records.");

        } finally {
            closeResolver(resolver);
        }
    }


    // =====================================================
    // POST: CREATE, UPDATE, DELETE
    // =====================================================

    @Override
    protected void doPost(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response)
            throws ServletException, IOException {

        prepareResponse(response);

        String action = clean(request.getParameter("action"));

        // Resolver is closed in "finally", AFTER the catch blocks run,
        // so the session is still alive when we discard changes.
        ResourceResolver resolver = null;
        Session session = null;

        try {
            resolver = getServiceResolver();
            session = getSession(resolver);

            logServiceUserDiagnostics(resolver, session);

            if (!session.nodeExists(CONTACTS_PATH)) {
                sendError(response, 500,
                    "Contact repository is unavailable.");
                return;
            }

            switch (action) {

                case "create":
                    createContact(request, response, session);
                    break;

                case "update":
                    updateContact(request, response, session);
                    break;

                case "delete":
                    deleteContact(request, response, session);
                    break;

                default:
                    sendError(response, 400,
                        "Invalid action. Use create, update or delete.");
                    break;
            }

        } catch (LoginException e) {

            LOG.error("TASK-9: Service resolver login failed.", e);
            sendError(response, 500,
                "Unable to obtain contact service resolver.");

        } catch (AccessDeniedException e) {

            discardPendingChanges(session);

            LOG.error("TASK-9: Repository denied the {} operation.",
                action, e);

            sendError(response, 403,
                "Repository permission denied. Check the service-user ACL.");

        } catch (RepositoryException e) {

            discardPendingChanges(session);

            LOG.error("TASK-9: Repository error during {} operation.",
                action, e);

            sendError(response, 500,
                "Contact operation failed. Check AEM error.log.");

        } catch (RuntimeException e) {

            discardPendingChanges(session);

            LOG.error("TASK-9: Unexpected error during {} operation.",
                action, e);

            sendError(response, 500,
                "Unexpected contact operation error.");

        } finally {
            closeResolver(resolver);
        }
    }


    // =====================================================
    // SERVICE USER DIAGNOSTICS
    // =====================================================

    private void logServiceUserDiagnostics(
            ResourceResolver resolver,
            Session session) {

        LOG.info("========== TASK-9 SERVICE USER DIAGNOSTICS ==========");
        LOG.info("Resolver user ID: {}", resolver.getUserID());
        LOG.info("JCR user ID: {}", session.getUserID());

        try {
            LOG.info("Contacts folder exists: {}",
                session.nodeExists(CONTACTS_PATH));

            String probe = CONTACTS_PATH + "/diagnostic-probe";

            LOG.info("read on {} = {}", CONTACTS_PATH,
                session.hasPermission(CONTACTS_PATH, "read"));
            LOG.info("add_node on {} = {}", probe,
                session.hasPermission(probe, "add_node"));
            LOG.info("set_property on {} = {}", probe,
                session.hasPermission(probe, "set_property"));
            LOG.info("remove on {} = {}", probe,
                session.hasPermission(probe, "remove"));

            AccessControlManager acm = session.getAccessControlManager();

            String[] privileges = {
                "jcr:read",
                "jcr:addChildNodes",
                "jcr:modifyProperties",
                "jcr:removeChildNodes",
                "jcr:removeNode",
                "jcr:nodeTypeManagement"
            };

            for (String privilegeName : privileges) {
                try {
                    Privilege privilege = acm.privilegeFromName(privilegeName);
                    boolean granted = acm.hasPrivileges(
                        CONTACTS_PATH, new Privilege[] { privilege });
                    LOG.info("Privilege {} = {}", privilegeName, granted);
                } catch (RepositoryException e) {
                    LOG.warn("Could not check privilege {}",
                        privilegeName, e);
                }
            }

            Node root = session.getNode(CONTACTS_PATH);
            LOG.info("Contacts folder primary type: {}",
                root.getPrimaryNodeType().getName());

        } catch (RepositoryException e) {
            LOG.error("TASK-9: Permission diagnostic failed.", e);
        }

        LOG.info("========== END SERVICE USER DIAGNOSTICS ==========");
    }


    // =====================================================
    // CREATE CONTACT
    // =====================================================

    private void createContact(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response,
            Session session)
            throws RepositoryException, IOException {

        String name = clean(request.getParameter("name"));
        String email = clean(request.getParameter("email"));
        String mobile = clean(request.getParameter("mobile"));

        if (!validateContact(name, email, mobile, response)) {
            return;
        }

        LOG.info("========== TASK-9 CREATE CONTACT ==========");

        String id = UUID.randomUUID().toString();
        String nodeName = "contact-" + id;
        String contactPath = CONTACTS_PATH + "/" + nodeName;

        if (!session.hasPermission(contactPath, "add_node")) {
            LOG.error("add_node denied. User={}, path={}",
                session.getUserID(), contactPath);
            sendError(response, 403,
                "Service user cannot create a contact under the contacts folder.");
            return;
        }

        Node root = session.getNode(CONTACTS_PATH);
        Node contact = addContactNode(session, root, nodeName, contactPath);

        contact.setProperty("id", id);
        contact.setProperty("name", name);
        contact.setProperty("email", email);
        contact.setProperty("mobile", mobile);

        session.save();

        LOG.info("TASK-9: Contact created successfully. ID={}", id);

        sendSuccess(response, "Contact created successfully.", id);

        LOG.info("========== TASK-9 CREATE COMPLETE ==========");
    }


    /**
     * Adds the contact node. First tries nt:unstructured. If Oak denies
     * that, logs diagnostics and retries with the parent folder's default
     * child node type (no explicit type).
     */
    private Node addContactNode(
            Session session,
            Node root,
            String nodeName,
            String contactPath)
            throws RepositoryException {

        try {
            return root.addNode(nodeName, "nt:unstructured");

        } catch (AccessDeniedException e) {

            LOG.warn("TASK-9: addNode with explicit type nt:unstructured "
                + "was denied. Message: {}", e.getMessage());

            logAddNodeFailureDiagnostics(session, contactPath);

            // Throw away the failed attempt, then retry without a type.
            discardPendingChanges(session);

            Node freshRoot = session.getNode(CONTACTS_PATH);

            Node contact = freshRoot.addNode(nodeName);

            LOG.info("TASK-9: Created contact using default child type: {}",
                contact.getPrimaryNodeType().getName());

            return contact;
        }
    }


    private void logAddNodeFailureDiagnostics(
            Session session,
            String contactPath) {

        LOG.warn("---------- TASK-9 ADD NODE FAILURE DIAGNOSTICS ----------");

        try {
            for (String a : new String[] {
                    "read", "add_node", "set_property", "remove" }) {
                LOG.warn("DIAG {} on {} = {}", a, contactPath,
                    session.hasPermission(contactPath, a));
            }

            AccessControlManager acm = session.getAccessControlManager();

            for (String p : new String[] {
                    "jcr:addChildNodes",
                    "jcr:nodeTypeManagement",
                    "jcr:modifyProperties" }) {
                LOG.warn("DIAG privilege {} on {} = {}", p, CONTACTS_PATH,
                    acm.hasPrivileges(CONTACTS_PATH,
                        new Privilege[] { acm.privilegeFromName(p) }));
            }

        } catch (RepositoryException ex) {
            LOG.warn("TASK-9: Diagnostics failed.", ex);
        }

        LOG.warn("---------- END ADD NODE FAILURE DIAGNOSTICS ----------");
    }


    // =====================================================
    // UPDATE CONTACT
    // =====================================================

    private void updateContact(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response,
            Session session)
            throws RepositoryException, IOException {

        String id = clean(request.getParameter("id"));
        String name = clean(request.getParameter("name"));
        String email = clean(request.getParameter("email"));
        String mobile = clean(request.getParameter("mobile"));

        if (!validateId(id, response)) {
            return;
        }

        if (!validateContact(name, email, mobile, response)) {
            return;
        }

        String contactPath = CONTACTS_PATH + "/contact-" + id;

        if (!session.nodeExists(contactPath)) {
            sendError(response, 404, "Contact not found.");
            return;
        }

        if (!session.hasPermission(contactPath, "set_property")) {
            sendError(response, 403,
                "Service user cannot modify this contact.");
            return;
        }

        Node contact = session.getNode(contactPath);

        contact.setProperty("name", name);
        contact.setProperty("email", email);
        contact.setProperty("mobile", mobile);

        session.save();

        LOG.info("TASK-9: Contact updated successfully. ID={}", id);

        sendSuccess(response, "Contact updated successfully.", id);
    }


    // =====================================================
    // DELETE CONTACT
    // =====================================================

    private void deleteContact(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response,
            Session session)
            throws RepositoryException, IOException {

        String id = clean(request.getParameter("id"));

        if (!validateId(id, response)) {
            return;
        }

        String contactPath = CONTACTS_PATH + "/contact-" + id;

        if (!session.nodeExists(contactPath)) {
            sendError(response, 404, "Contact not found.");
            return;
        }

        if (!session.hasPermission(contactPath, "remove")) {
            sendError(response, 403,
                "Service user cannot delete this contact.");
            return;
        }

        session.getNode(contactPath).remove();
        session.save();

        LOG.info("TASK-9: Contact deleted successfully. ID={}", id);

        sendSuccess(response, "Contact deleted successfully.", id);
    }


    // =====================================================
    // VALIDATION
    // =====================================================

    private boolean validateContact(
            String name,
            String email,
            String mobile,
            SlingHttpServletResponse response)
            throws IOException {

        if (name.isEmpty() || name.length() > 100) {
            sendError(response, 400,
                "Name is required and must not exceed 100 characters.");
            return false;
        }

        if (email.length() > 254
                || !EMAIL_PATTERN.matcher(email).matches()) {
            sendError(response, 400,
                "Please enter a valid email address.");
            return false;
        }

        if (!MOBILE_PATTERN.matcher(mobile).matches()) {
            sendError(response, 400,
                "Please enter a valid mobile number.");
            return false;
        }

        String digits = mobile.replaceAll("[^0-9]", "");

        if (digits.length() < 7 || digits.length() > 15) {
            sendError(response, 400,
                "Mobile number must contain between 7 and 15 digits.");
            return false;
        }

        return true;
    }


    private boolean validateId(
            String id,
            SlingHttpServletResponse response)
            throws IOException {

        if (id.isEmpty() || !UUID_PATTERN.matcher(id).matches()) {
            sendError(response, 400, "Invalid contact ID.");
            return false;
        }

        return true;
    }


    // =====================================================
    // SERVICE RESOURCE RESOLVER
    // =====================================================

    private ResourceResolver getServiceResolver()
            throws LoginException {

        return resolverFactory.getServiceResourceResolver(
            Collections.singletonMap(
                ResourceResolverFactory.SUBSERVICE, SUBSERVICE));
    }


    private Session getSession(ResourceResolver resolver)
            throws RepositoryException {

        Session session = resolver.adaptTo(Session.class);

        if (session == null) {
            throw new RepositoryException(
                "Could not adapt service resolver to JCR Session.");
        }

        return session;
    }


    private void closeResolver(ResourceResolver resolver) {
        if (resolver != null && resolver.isLive()) {
            resolver.close();
        }
    }


    // =====================================================
    // CONTACT HELPERS
    // =====================================================

    private boolean hasContactProperties(Node contact)
            throws RepositoryException {

        return contact.hasProperty("id")
            && contact.hasProperty("name")
            && contact.hasProperty("email")
            && contact.hasProperty("mobile");
    }


    private String contactToJson(Node contact)
            throws RepositoryException {

        return "{"
            + "\"id\":\""
            + jsonEscape(contact.getProperty("id").getString()) + "\","
            + "\"name\":\""
            + jsonEscape(contact.getProperty("name").getString()) + "\","
            + "\"email\":\""
            + jsonEscape(contact.getProperty("email").getString()) + "\","
            + "\"mobile\":\""
            + jsonEscape(contact.getProperty("mobile").getString()) + "\""
            + "}";
    }


    // =====================================================
    // ROLLBACK PENDING SESSION CHANGES
    // =====================================================

    private void discardPendingChanges(Session session) {

        if (session == null || !session.isLive()) {
            return;
        }

        try {
            if (session.hasPendingChanges()) {
                session.refresh(false);
                LOG.info("TASK-9: Pending repository changes discarded.");
            }
        } catch (RepositoryException e) {
            LOG.error("TASK-9: Could not discard pending changes.", e);
        }
    }


    // =====================================================
    // RESPONSE HELPERS
    // =====================================================

    private void prepareResponse(SlingHttpServletResponse response) {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
    }


    private void sendSuccess(
            SlingHttpServletResponse response,
            String message,
            String id)
            throws IOException {

        response.setStatus(200);

        response.getWriter().write(
            "{\"success\":true,"
            + "\"message\":\"" + jsonEscape(message) + "\","
            + "\"id\":\"" + jsonEscape(id) + "\"}");
    }


    private void sendError(
            SlingHttpServletResponse response,
            int status,
            String message)
            throws IOException {

        response.setStatus(status);

        response.getWriter().write(
            "{\"success\":false,"
            + "\"message\":\"" + jsonEscape(message) + "\"}");
    }


    // =====================================================
    // UTILITY METHODS
    // =====================================================

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }


    private String jsonEscape(String value) {

        if (value == null) {
            return "";
        }

        StringBuilder escaped = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {

            char c = value.charAt(i);

            switch (c) {
                case '"':  escaped.append("\\\""); break;
                case '\\': escaped.append("\\\\"); break;
                case '\b': escaped.append("\\b");  break;
                case '\f': escaped.append("\\f");  break;
                case '\n': escaped.append("\\n");  break;
                case '\r': escaped.append("\\r");  break;
                case '\t': escaped.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                    break;
            }
        }

        return escaped.toString();
    }
}