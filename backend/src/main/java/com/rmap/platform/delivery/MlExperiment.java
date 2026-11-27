package com.rmap.platform.delivery;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Document(collection = "ml_experiments")
public class MlExperiment {
    @Id
    private String id;
    @Indexed
    private String projectId;
    private String name;
    private String taskType; // CLASSIFICATION, REGRESSION, CLUSTERING, NLP, COMPUTER_VISION, TIME_SERIES, CUSTOM
    private String datasetFingerprint;
    private String modelFamily;
    private int randomSeed = 42;
    private String libraryVersions;
    private Map<String, Object> hyperparameters = new LinkedHashMap<>();
    private String status; // PLANNED, RESULTS_IMPORTED
    private String resultSource; // Must reference real script/notebook run — never fabricated
    private Map<String, Double> importedMetrics = new LinkedHashMap<>();
    private String notes;
    @CreatedDate
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public String getDatasetFingerprint() { return datasetFingerprint; }
    public void setDatasetFingerprint(String datasetFingerprint) { this.datasetFingerprint = datasetFingerprint; }
    public String getModelFamily() { return modelFamily; }
    public void setModelFamily(String modelFamily) { this.modelFamily = modelFamily; }
    public int getRandomSeed() { return randomSeed; }
    public void setRandomSeed(int randomSeed) { this.randomSeed = randomSeed; }
    public String getLibraryVersions() { return libraryVersions; }
    public void setLibraryVersions(String libraryVersions) { this.libraryVersions = libraryVersions; }
    public Map<String, Object> getHyperparameters() { return hyperparameters; }
    public void setHyperparameters(Map<String, Object> hyperparameters) { this.hyperparameters = hyperparameters; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getResultSource() { return resultSource; }
    public void setResultSource(String resultSource) { this.resultSource = resultSource; }
    public Map<String, Double> getImportedMetrics() { return importedMetrics; }
    public void setImportedMetrics(Map<String, Double> importedMetrics) { this.importedMetrics = importedMetrics; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
}
