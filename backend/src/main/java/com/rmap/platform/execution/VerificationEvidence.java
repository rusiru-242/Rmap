package com.rmap.platform.execution;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "verification_evidence")
public class VerificationEvidence {
    @Id
    private String id;
    @Indexed
    private String projectId;
    @Indexed
    private String requirementId;
    private int specVersion;
    private String sourceOrCommand;
    private String evidenceStrength; // FILE_EXISTS, AGENT_CLAIM, BUILD_PASS, TEST_PASS, API_VERIFIED, MANUAL_APPROVED
    private String gitRevision;
    private String actualResult;
    private String verificationStatus; // VERIFIED, CLAIM_UNVERIFIED, FAILED, STALE
    private String recordedBy;
    private Instant recordedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getRequirementId() { return requirementId; }
    public void setRequirementId(String requirementId) { this.requirementId = requirementId; }
    public int getSpecVersion() { return specVersion; }
    public void setSpecVersion(int specVersion) { this.specVersion = specVersion; }
    public String getSourceOrCommand() { return sourceOrCommand; }
    public void setSourceOrCommand(String sourceOrCommand) { this.sourceOrCommand = sourceOrCommand; }
    public String getEvidenceStrength() { return evidenceStrength; }
    public void setEvidenceStrength(String evidenceStrength) { this.evidenceStrength = evidenceStrength; }
    public String getGitRevision() { return gitRevision; }
    public void setGitRevision(String gitRevision) { this.gitRevision = gitRevision; }
    public String getActualResult() { return actualResult; }
    public void setActualResult(String actualResult) { this.actualResult = actualResult; }
    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }
    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }
    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }
}
