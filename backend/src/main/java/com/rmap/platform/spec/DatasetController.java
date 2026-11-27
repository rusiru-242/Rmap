package com.rmap.platform.spec;

import com.rmap.platform.auth.AuthenticatedUser;
import com.rmap.platform.billing.RequiresActiveEntitlement;
import com.rmap.platform.common.ApiException;
import com.rmap.platform.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/datasets")
public class DatasetController {
    private final DatasetIntelligenceService service;

    public DatasetController(DatasetIntelligenceService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresActiveEntitlement
    public ApiResponse<DatasetProfile> uploadAndProfile(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String targetColumn,
            @RequestParam(required = false) String mlTaskType
    ) {
        try {
            return ApiResponse.ok(service.profileCsv(
                    projectId, user, file.getOriginalFilename(), file.getBytes(), targetColumn, mlTaskType
            ));
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DATASET_READ_FAILED", "Failed to read dataset file.");
        }
    }

    @GetMapping
    public ApiResponse<List<DatasetProfile>> list(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(service.list(projectId, user));
    }
}
