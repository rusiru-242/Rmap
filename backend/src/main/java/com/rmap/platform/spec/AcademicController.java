package com.rmap.platform.spec;

import com.rmap.platform.auth.AuthenticatedUser;
import com.rmap.platform.billing.RequiresActiveEntitlement;
import com.rmap.platform.common.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/academic")
public class AcademicController {
    private final AcademicService academicService;

    public AcademicController(AcademicService academicService) {
        this.academicService = academicService;
    }

    @GetMapping
    public ApiResponse<AcademicAssessment> get(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(academicService.getOrCreate(projectId, user));
    }

    @PutMapping
    @RequiresActiveEntitlement
    public ApiResponse<AcademicAssessment> update(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody AcademicAssessment assessment
    ) {
        return ApiResponse.ok(academicService.saveAndAudit(projectId, user, assessment));
    }

    @GetMapping("/rubric-coverage")
    public ApiResponse<Map<String, Object>> coverage(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(academicService.rubricCoverageSummary(projectId, user));
    }
}
