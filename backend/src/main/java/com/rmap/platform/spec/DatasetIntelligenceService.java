package com.rmap.platform.spec;

import com.rmap.platform.auth.AuthenticatedUser;
import com.rmap.platform.common.ApiException;
import com.rmap.platform.project.ProjectAuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class DatasetIntelligenceService {
    private static final int MAX_ROWS_PROFILED = 10_000;

    private final DatasetProfileRepository repository;
    private final ProjectAuthorizationService projectAuth;

    public DatasetIntelligenceService(DatasetProfileRepository repository, ProjectAuthorizationService projectAuth) {
        this.repository = repository;
        this.projectAuth = projectAuth;
    }

    public DatasetProfile profileCsv(
            String projectId,
            AuthenticatedUser user,
            String fileName,
            byte[] csvBytes,
            String targetColumn,
            String mlTaskType
    ) {
        projectAuth.requireEditor(projectId, user);
        if (csvBytes == null || csvBytes.length == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_DATASET", "Dataset file is empty.");
        }

        String text = new String(csvBytes, StandardCharsets.UTF_8);
        String[] lines = text.split("\\r?\\n");
        if (lines.length < 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CSV", "CSV must contain a header row and at least one data row.");
        }

        String[] headers = splitCsvLine(lines[0]);
        int colCount = headers.length;
        int rowLimit = Math.min(lines.length - 1, MAX_ROWS_PROFILED);

        List<List<String>> rows = new ArrayList<>();
        Set<String> seenRows = new HashSet<>();
        int duplicates = 0;

        for (int r = 1; r <= rowLimit; r++) {
            String line = lines[r].trim();
            if (line.isEmpty()) continue;
            if (!seenRows.add(line)) {
                duplicates++;
            }
            String[] vals = splitCsvLine(line);
            List<String> row = new ArrayList<>(colCount);
            for (int c = 0; c < colCount; c++) {
                row.add(c < vals.length ? vals[c].trim() : "");
            }
            rows.add(row);
        }

        List<DatasetProfile.ColumnStat> colStats = new ArrayList<>();
        List<String> qualityWarnings = new ArrayList<>();
        List<String> leakageWarnings = new ArrayList<>();

        if (duplicates > 0) {
            qualityWarnings.add("Detected " + duplicates + " duplicate row(s). Deduplicate before train/test splitting to avoid leakage.");
        }

        for (int c = 0; c < colCount; c++) {
            String colName = headers[c].trim();
            int missing = 0;
            Set<String> distinct = new HashSet<>();
            List<Double> numericVals = new ArrayList<>();
            boolean allNonEmptyNumeric = true;

            for (List<String> row : rows) {
                String v = row.get(c);
                if (v.isEmpty() || v.equalsIgnoreCase("null") || v.equalsIgnoreCase("na") || v.equalsIgnoreCase("nan")) {
                    missing++;
                } else {
                    distinct.add(v);
                    try {
                        numericVals.add(Double.parseDouble(v));
                    } catch (NumberFormatException ex) {
                        allNonEmptyNumeric = false;
                    }
                }
            }

            double missingPct = rows.isEmpty() ? 0.0 : (missing * 100.0) / rows.size();
            String inferredType = (allNonEmptyNumeric && !numericVals.isEmpty()) ? "NUMERIC" : "CATEGORICAL_OR_TEXT";
            Double min = null, max = null, mean = null;
            if ("NUMERIC".equals(inferredType)) {
                min = numericVals.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
                max = numericVals.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
                mean = numericVals.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            }

            colStats.add(new DatasetProfile.ColumnStat(
                    colName, inferredType, missing, Math.round(missingPct * 100.0) / 100.0, distinct.size(), min, max, mean
            ));

            if (missingPct >= 15.0) {
                qualityWarnings.add("Column '" + colName + "' has " + String.format(Locale.ROOT, "%.1f%%", missingPct) + " missing values.");
            }
            String lower = colName.toLowerCase(Locale.ROOT);
            if (lower.contains("leak") || lower.contains("future") || lower.contains("post_") || lower.equals("id") || lower.endsWith("_id")) {
                if (targetColumn == null || !colName.equalsIgnoreCase(targetColumn)) {
                    leakageWarnings.add("Column '" + colName + "' may introduce target leakage or ID memorization — review before feature selection.");
                }
            }
        }

        String resolvedTask = (mlTaskType == null || mlTaskType.isBlank()) ? "CLASSIFICATION" : mlTaskType.toUpperCase(Locale.ROOT);
        DatasetProfile profile = new DatasetProfile();
        profile.setProjectId(projectId);
        profile.setFileName(fileName);
        profile.setSha256Fingerprint(sha256(csvBytes));
        profile.setSizeBytes(csvBytes.length);
        profile.setRowCount(rows.size());
        profile.setColumnCount(colCount);
        profile.setDuplicateRowCount(duplicates);
        profile.setTargetColumn(targetColumn);
        profile.setMlTaskType(resolvedTask);
        profile.setColumns(colStats);
        profile.setDataQualityWarnings(qualityWarnings);
        profile.setLeakageRiskWarnings(leakageWarnings);
        profile.setExperimentPlan(buildPlan(resolvedTask));

        return repository.save(profile);
    }

    public List<DatasetProfile> list(String projectId, AuthenticatedUser user) {
        projectAuth.requireMember(projectId, user);
        return repository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    private DatasetProfile.MlExperimentPlan buildPlan(String taskType) {
        return switch (taskType) {
            case "REGRESSION" -> new DatasetProfile.MlExperimentPlan(
                    "REGRESSION",
                    List.of("Train/val/test split before fitting scalers/imputers", "Median/mode imputation", "StandardScaler for linear models"),
                    List.of("DummyRegressor (mean)", "Ridge Regression"),
                    List.of("RandomForestRegressor", "HistGradientBoostingRegressor"),
                    "5-fold Cross-Validation with fixed random seed",
                    List.of("RMSE", "MAE", "R2"),
                    List.of("Check subgroup residual bias", "Document dataset provenance and known limitations")
            );
            case "CLUSTERING" -> new DatasetProfile.MlExperimentPlan(
                    "CLUSTERING",
                    List.of("Missing value imputation", "Feature scaling (StandardScaler)", "Outlier inspection"),
                    List.of("K-Means"),
                    List.of("HDBSCAN / DBSCAN", "Gaussian Mixture Models"),
                    "Stability across seeds & Silhouette / Davies-Bouldin analysis",
                    List.of("Silhouette Score", "Davies-Bouldin Index"),
                    List.of("Verify clusters do not encode proxy protected attributes")
            );
            default -> new DatasetProfile.MlExperimentPlan(
                    taskType,
                    List.of("Stratified train/val/test split prior to preprocessing", "Imputation fitted strictly on training fold", "One-hot / ordinal encoding"),
                    List.of("DummyClassifier (most_frequent)", "LogisticRegression"),
                    List.of("RandomForestClassifier", "HistGradientBoostingClassifier"),
                    "Stratified 5-fold Cross-Validation with fixed random seed",
                    List.of("Macro F1", "ROC-AUC", "Precision / Recall", "Confusion Matrix"),
                    List.of("Evaluate class imbalance & subgroup fairness", "Never report unexecuted or fabricated model accuracy")
            );
        };
    }

    private String[] splitCsvLine(String line) {
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
