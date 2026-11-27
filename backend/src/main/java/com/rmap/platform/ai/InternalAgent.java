package com.rmap.platform.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.rmap.platform.document.ProjectDocument;
import com.rmap.platform.project.Project;

import java.util.List;
import java.util.Map;

public interface InternalAgent {
    AgentType type();
    String responsibilitySummary();
    String systemInstruction();
    List<String> requiredOutputFields();
    String buildUserPrompt(AgentContext context);
    void validateSchema(JsonNode output);

    record AgentContext(
            Project project,
            List<ProjectDocument> documents,
            Map<AgentType, JsonNode> priorAgentOutputs,
            String additionalInstruction
    ) {}
}
