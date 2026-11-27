package com.rmap.platform.delivery;

import com.rmap.platform.auth.AuthenticatedUser;
import com.rmap.platform.billing.RequiresActiveEntitlement;
import com.rmap.platform.common.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class DeliveryController {
    private final DeliveryService service;

    public DeliveryController(DeliveryService service) {
        this.service = service;
    }

    public record StitchPromptRequest(String targetSurface, String visualNotes) {}
    public record GenerateDocRequest(String docType) {}
    public record ImportMlResultsRequest(String resultSource, Map<String, Double> metrics, String notes) {}
    public record GenerateDeployRequest(String provider, String targetType, String rootDirectory, String buildCommand, String startCommand) {}
    public record ClientChangeRequest(String requirementId, String changeSummary) {}

    @GetMapping("/catalog/hosting")
    public ApiResponse<List<Map<String, Object>>> hostingCatalog() {
        return ApiResponse.ok(service.hostingCatalog());
    }

    @PostMapping("/projects/{projectId}/delivery/design/stitch-prompt")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresActiveEntitlement
    public ApiResponse<DesignArtifact> generateStitchPrompt(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody StitchPromptRequest req
    ) {
        return ApiResponse.ok(service.generateStitchPrompt(projectId, user, req.targetSurface(), req.visualNotes()));
    }

    @GetMapping("/projects/{projectId}/delivery/design")
    public ApiResponse<List<DesignArtifact>> listDesign(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(service.listDesignArtifacts(projectId, user));
    }

    @PostMapping("/projects/{projectId}/delivery/docs")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresActiveEntitlement
    public ApiResponse<GeneratedDocument> generateDoc(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody GenerateDocRequest req
    ) {
        return ApiResponse.ok(service.generateDocumentation(projectId, user, req.docType()));
    }

    @GetMapping("/projects/{projectId}/delivery/docs")
    public ApiResponse<List<GeneratedDocument>> listDocs(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(service.listDocuments(projectId, user));
    }

    @GetMapping("/projects/{projectId}/delivery/docs/{docId}/export")
    public ResponseEntity<byte[]> exportDoc(
            @PathVariable String projectId,
            @PathVariable String docId,
            @RequestParam(defaultValue = "MD") String format,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        byte[] bytes = service.exportDocumentBytes(projectId, docId, user, format);
        MediaType mt = switch (format.toUpperCase()) {
            case "PDF" -> MediaType.APPLICATION_PDF;
            case "DOCX" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            default -> MediaType.TEXT_MARKDOWN;
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rmap-doc." + format.toLowerCase() + "\"")
                .contentType(mt)
                .body(bytes);
    }

    @PostMapping("/projects/{projectId}/delivery/ml/experiments")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresActiveEntitlement
    public ApiResponse<MlExperiment> createExperiment(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody MlExperiment req
    ) {
        return ApiResponse.ok(service.createExperiment(projectId, user, req));
    }

    @PostMapping("/projects/{projectId}/delivery/ml/experiments/{expId}/results")
    @RequiresActiveEntitlement
    public ApiResponse<MlExperiment> importMlResults(
            @PathVariable String projectId,
            @PathVariable String expId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody ImportMlResultsRequest req
    ) {
        return ApiResponse.ok(service.importRealExperimentResults(projectId, expId, user, req.resultSource(), req.metrics(), req.notes()));
    }

    @GetMapping("/projects/{projectId}/delivery/ml/experiments")
    public ApiResponse<List<MlExperiment>> listExperiments(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(service.listExperiments(projectId, user));
    }

    @PostMapping("/projects/{projectId}/delivery/deployments")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresActiveEntitlement
    public ApiResponse<DeploymentPlan> generateDeployment(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody GenerateDeployRequest req
    ) {
        return ApiResponse.ok(service.generateDeploymentPlan(
                projectId, user, req.provider(), req.targetType(), req.rootDirectory(), req.buildCommand(), req.startCommand()
        ));
    }

    @GetMapping("/projects/{projectId}/delivery/deployments")
    public ApiResponse<List<DeploymentPlan>> listDeployments(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(service.listDeploymentPlans(projectId, user));
    }

    @GetMapping("/projects/{projectId}/delivery/academic-pack")
    public ApiResponse<Map<String, Object>> academicPack(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(service.getAcademicDeliveryPack(projectId, user));
    }

    @GetMapping("/projects/{projectId}/delivery/testing-security")
    public ApiResponse<Map<String, Object>> testingAndSecurity(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(service.getTestingAndSecurityGuidance(projectId, user));
    }

    @PostMapping("/projects/{projectId}/delivery/change-requests")
    @RequiresActiveEntitlement
    public ApiResponse<Map<String, Object>> changeRequest(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody ClientChangeRequest req
    ) {
        return ApiResponse.ok(service.processClientChangeRequest(projectId, user, req.requirementId(), req.changeSummary()));
    }
}
