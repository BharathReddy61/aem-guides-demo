package com.adobe.aem.guides.demo.core.workflow;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;

@Component(
    service = WorkflowProcess.class,
    property = {
        "process.label=Demo Workflow Process"
    }
)
public class demoWorkflow implements WorkflowProcess {

    private static final Logger LOG =
            LoggerFactory.getLogger(demoWorkflow.class);

    @Override
    public void execute(
            WorkItem workItem,
            WorkflowSession workflowSession,
            MetaDataMap args) {

        Object payloadObject = workItem.getWorkflowData().getPayload();

        String payload = payloadObject != null
                ? payloadObject.toString()
                : "No payload available";

        LOG.info("Workflow triggered successfully");
        LOG.info("Payload: {}", payload);
    }
}
