package com.rmap.platform.delivery;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "design_artifacts")
public class DesignArtifact {
    @Id
    private String id;
    @Indexed
    private String projectId;
    private String artifactType; // DESIGN_SYSTEM, STITCH_PROMPT, DESIGN_ZIP_ANALYSIS
    private String targetSurface; // WEB, DESKTOP, MOBILE, ADMIN, LANDING, SCREEN
    private String title;
    private String stitchPrompt;
    private List<String> pagesInventory = new ArrayList<>();
    private List<String> componentMappings = new ArrayList<>();
    private List<String> preservationRules = new ArrayList<>();
    @CreatedDate
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getArtifactType() { return artifactType; }
    public void setArtifactType(String artifactType) { this.artifactType = artifactType; }
    public String getTargetSurface() { return targetSurface; }
    public void setTargetSurface(String targetSurface) { this.targetSurface = targetSurface; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStitchPrompt() { return stitchPrompt; }
    public void setStitchPrompt(String stitchPrompt) { this.stitchPrompt = stitchPrompt; }
    public List<String> getPagesInventory() { return pagesInventory; }
    public void setPagesInventory(List<String> pagesInventory) { this.pagesInventory = pagesInventory; }
    public List<String> getComponentMappings() { return componentMappings; }
    public void setComponentMappings(List<String> componentMappings) { this.componentMappings = componentMappings; }
    public List<String> getPreservationRules() { return preservationRules; }
    public void setPreservationRules(List<String> preservationRules) { this.preservationRules = preservationRules; }
    public Instant getCreatedAt() { return createdAt; }
}
