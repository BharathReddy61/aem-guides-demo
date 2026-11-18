
package com.adobe.aem.guides.demo.core.workflow;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.ParticipantStepChooser;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.metadata.MetaDataMap;

@Component(
    service = ParticipantStepChooser.class,
    immediate = true,
    property = {
        "chooser.label=Task 13 - Dynamic Participant Chooser"
    }
)
public class Task13DynamicParticipantChooser
        implements ParticipantStepChooser {

    private static final Logger LOG =
        LoggerFactory.getLogger(Task13DynamicParticipantChooser.class);

    @Override
    public String getParticipant(
            WorkItem workItem,
            WorkflowSession workflowSession,
            MetaDataMap args) throws WorkflowException {

        if (workItem == null || workItem.getWorkflowData() == null) {
            throw new WorkflowException(
                "Task-13: Workflow data is unavailable."
            );
        }

        Object payload = workItem.getWorkflowData().getPayload();

        if (payload == null) {
            throw new WorkflowException(
                "Task-13: Workflow payload is empty."
            );
        }

        String payloadPath = payload.toString();
        String initiator = workItem.getWorkflow().getInitiator();

        String participant;

        if (payloadPath.startsWith("/content/demo/us/en")) {
            participant = "admin";
        } else if (initiator != null && !initiator.trim().isEmpty()
                && !"workflow-service".equals(initiator)) {
            participant = initiator;
        } else {
            participant = "admin";
        }

        LOG.info(
            "Task-13: Payload={}, Initiator={}, Selected participant={}",
            payloadPath,
            initiator,
            participant
        );

        return participant;
    }
}
