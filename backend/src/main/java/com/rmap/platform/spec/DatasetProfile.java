package com.rmap.platform.spec;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "dataset_profiles")
public class DatasetProfile {
    @Id
    private String id;
    @Indexed
    private String projectId;
    private String fileName;
    private String sha256Fingerprint;
    private long sizeBytes;
    private int rowCount;
    private int columnCount;
    private int duplicateRowCount;
    private String targetColumn;
    private String mlTaskType; // CLASSIFICATION, REGRESSION, CLUSTERING, NLP, COMPUTER_VISION, TIME_SERIES, CUSTOM
    private List<ColumnStat> columns = new ArrayList<>();
    private List<String> dataQualityWarnings = new ArrayList<>();
    private List<String> leakageRiskWarnings = new ArrayList<>();
    private MlExperimentPlan experimentPlan;
    @CreatedDate
    private Instant createdAt;

    public record ColumnStat(
            String name,
            String inferredType,
            int missingCount,
            double missingPercent,
            int distinctCount,
            Double min,
            Double max,
            Double mean
    ) {}

    public record MlExperimentPlan(
            String taskType,
            List<String> preprocessingSteps,
            List<String> baselineModels,
            List<String> candidateModels,
            String validationStrategy,
            List<String> evaluationMetrics,
            List<String> ethicalConsiderations
    ) {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getSha256Fingerprint() { return sha256Fingerprint; }
    public void setSha256Fingerprint(String sha256Fingerprint) { this.sha256Fingerprint = sha256Fingerprint; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
    public int getRowCount() { return rowCount; }
    public void setRowCount(int rowCount) { this.rowCount = rowCount; }
    public int getColumnCount() { return columnCount; }
    public void setColumnCount(int columnCount) { this.columnCount = columnCount; }
    public int getDuplicateRowCount() { return duplicateRowCount; }
    public void setDuplicateRowCount(int duplicateRowCount) { this.duplicateRowCount = duplicateRowCount; }
    public String getTargetColumn() { return targetColumn; }
    public void setTargetColumn(String targetColumn) { this.targetColumn = targetColumn; }
    public String getMlTaskType() { return mlTaskType; }
    public void setMlTaskType(String mlTaskType) { this.mlTaskType = mlTaskType; }
    public List<ColumnStat> getColumns() { return columns; }
    public void setColumns(List<ColumnStat> columns) { this.columns = columns; }
    public List<String> getDataQualityWarnings() { return dataQualityWarnings; }
    public void setDataQualityWarnings(List<String> dataQualityWarnings) { this.dataQualityWarnings = dataQualityWarnings; }
    public List<String> getLeakageRiskWarnings() { return leakageRiskWarnings; }
    public void setLeakageRiskWarnings(List<String> leakageRiskWarnings) { this.leakageRiskWarnings = leakageRiskWarnings; }
    public MlExperimentPlan getExperimentPlan() { return experimentPlan; }
    public void setExperimentPlan(MlExperimentPlan experimentPlan) { this.experimentPlan = experimentPlan; }
    public Instant getCreatedAt() { return createdAt; }
}
