
package com.adobe.aem.guides.demo.core.workflow;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.adobe.granite.workflow.WorkflowSession; 
@Component(
    service = WorkflowProcess.class,
    property = {
        "process.label=Task 10 - Set Page Expiry Date"
    }
)
public class Task10PageExpiryProcess implements WorkflowProcess {

    private static final Logger LOG =
        LoggerFactory.getLogger(Task10PageExpiryProcess.class);

    private static final String PROJECT_TEMPLATE_ROOT =
        "/conf/demo/settings/wcm/templates/";

    @Override
    public void execute(
            WorkItem workItem,
            WorkflowSession workflowSession,
            MetaDataMap args) throws WorkflowException {

        String payloadPath = String.valueOf(
            workItem.getWorkflowData().getPayload()
        );

        Session session = workflowSession.adaptTo(Session.class);

        if (session == null) {
            throw new WorkflowException(
                "Unable to obtain JCR Session from WorkflowSession"
            );
        }

        try {
            if (!session.nodeExists(payloadPath + "/jcr:content")) {
                throw new WorkflowException(
                    "Page content node does not exist: " + payloadPath
                );
            }

            Node contentNode =
                session.getNode(payloadPath + "/jcr:content");

            if (!contentNode.hasProperty("cq:template")) {
                throw new WorkflowException(
                    "cq:template is missing for page: " + payloadPath
                );
            }

            String templatePath =
                contentNode.getProperty("cq:template").getString();

            Calendar expiryDate;

            if (templatePath.startsWith(PROJECT_TEMPLATE_ROOT)) {

                // Own project template: expiryDate = current date/time.
                expiryDate = Calendar.getInstance();

                LOG.info(
                    "Task-10: Own project template detected. Page: {}",
                    payloadPath
                );

            } else {

                // Other project template: use this page's own creation time.
                if (!contentNode.hasProperty("jcr:created")) {
                    throw new WorkflowException(
                        "jcr:created is missing for page: " + payloadPath
                    );
                }

                expiryDate =
                    contentNode.getProperty("jcr:created").getDate();

                LOG.info(
                    "Task-10: Other project template detected. " +
                    "Using page creation date. Page: {}",
                    payloadPath
                );
            }

            // JCR Calendar values are stored as DATE properties.
            contentNode.setProperty("expiryDate", expiryDate);
            session.save();

            LOG.info(
                "Task-10: expiryDate successfully saved for {}",
                payloadPath
            );

        } catch (RepositoryException e) {
            LOG.error(
                "Task-10: Failed to update expiryDate for {}",
                payloadPath,
                e
            );

            throw new WorkflowException(
                "Task-10 failed for page: " + payloadPath,
                e
            );
        }
    }
}
