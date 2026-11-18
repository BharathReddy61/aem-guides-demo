
package com.adobe.aem.guides.demo.core.servlets;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;

import javax.jcr.Session;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.paths=/bin/task14/check-access",
        "sling.servlet.methods=GET"
    }
)
public class Task14AccessCheckServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");

        String userId = request.getResourceResolver().getUserID();

        if (userId == null || "anonymous".equals(userId)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("User is not authenticated.");
            return;
        }

        try {
            Session session = request.getResourceResolver().adaptTo(Session.class);

            if (session == null || !(session instanceof JackrabbitSession)) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("Unable to obtain Jackrabbit session.");
                return;
            }

            JackrabbitSession jackrabbitSession = (JackrabbitSession) session;

            UserManager userManager = jackrabbitSession.getUserManager();

            Authorizable user = userManager.getAuthorizable(userId);
            Authorizable groupAuthorizable =
                    userManager.getAuthorizable("Access");

            if (user == null || groupAuthorizable == null
                    || !groupAuthorizable.isGroup()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write(
                    "User or Access group was not found.");
                return;
            }

            Group accessGroup = (Group) groupAuthorizable;

            if (accessGroup.isMember(user)) {
                response.getWriter().write(
                    "User " + userId + " belongs to Access group.");
            } else {
                response.getWriter().write(
                    "User " + userId + " does not belong to Access group.");
            }

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error checking group membership: "
                    + e.getMessage());
            getServletContext().log(
                    "Task-14: Error checking Access group membership.", e);
        }
    }
}
