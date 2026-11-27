package com.rmap.platform.delivery;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "deployment_plans")
public class DeploymentPlan {
    @Id
    private String id;
    @Indexed
    private String projectId;
    private String provider;
    private String targetType;
    private String branchName;
    private String rootDirectory;
    private String buildCommand;
    private String startCommand;
    private List<String> requiredEnvVars = new ArrayList<>();
    private List<String> clickByClickManualSteps = new ArrayList<>();
    private String providerGuidanceNotice;
    private String liveUrl;
    private String verificationStatus; // PLANNED, VERIFIED_LIVE, FAILED
    @CreatedDate
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    public String getRootDirectory() { return rootDirectory; }
    public void setRootDirectory(String rootDirectory) { this.rootDirectory = rootDirectory; }
    public String getBuildCommand() { return buildCommand; }
    public void setBuildCommand(String buildCommand) { this.buildCommand = buildCommand; }
    public String getStartCommand() { return startCommand; }
    public void setStartCommand(String startCommand) { this.startCommand = startCommand; }
    public List<String> getRequiredEnvVars() { return requiredEnvVars; }
    public void setRequiredEnvVars(List<String> requiredEnvVars) { this.requiredEnvVars = requiredEnvVars; }
    public List<String> getClickByClickManualSteps() { return clickByClickManualSteps; }
    public void setClickByClickManualSteps(List<String> clickByClickManualSteps) { this.clickByClickManualSteps = clickByClickManualSteps; }
    public String getProviderGuidanceNotice() { return providerGuidanceNotice; }
    public void setProviderGuidanceNotice(String providerGuidanceNotice) { this.providerGuidanceNotice = providerGuidanceNotice; }
    public String getLiveUrl() { return liveUrl; }
    public void setLiveUrl(String liveUrl) { this.liveUrl = liveUrl; }
    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }
    public Instant getCreatedAt() { return createdAt; }
}
