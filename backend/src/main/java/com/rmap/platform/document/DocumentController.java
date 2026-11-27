package com.rmap.platform.document;

import com.rmap.platform.auth.AuthenticatedUser;
import com.rmap.platform.billing.RequiresActiveEntitlement;
import com.rmap.platform.common.ApiException;
import com.rmap.platform.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/documents")
public class DocumentController {
    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresActiveEntitlement
    public ApiResponse<ProjectDocument> upload(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            return ApiResponse.ok(documentService.upload(
                    projectId,
                    user,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getBytes()
            ));
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FILE_READ_FAILED", "Failed to read uploaded file.");
        }
    }

    @GetMapping
    public ApiResponse<List<ProjectDocument>> list(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(documentService.list(projectId, user));
    }

    @GetMapping("/{documentId}")
    public ApiResponse<ProjectDocument> get(
            @PathVariable String projectId,
            @PathVariable String documentId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(documentService.get(projectId, documentId, user));
    }

    @DeleteMapping("/{documentId}")
    @RequiresActiveEntitlement
    public ApiResponse<Map<String, String>> delete(
            @PathVariable String projectId,
            @PathVariable String documentId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        documentService.delete(projectId, documentId, user);
        return ApiResponse.ok(Map.of("deleted", documentId));
    }
}
