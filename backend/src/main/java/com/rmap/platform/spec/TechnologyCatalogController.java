package com.rmap.platform.spec;

import com.rmap.platform.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog/technologies")
public class TechnologyCatalogController {
    private final TechnologyCatalogService service;

    public TechnologyCatalogController(TechnologyCatalogService service) {
        this.service = service;
    }

    public record ValidateTechRequest(
            List<String> selectedTechnologies,
            List<String> mandatoryConstraints,
            List<String> forbiddenTechnologies
    ) {}

    @GetMapping
    public ApiResponse<List<TechnologyCatalogItem>> list(@RequestParam(required = false) String category) {
        return ApiResponse.ok(service.list(category));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TechnologyCatalogItem> addCustom(@RequestBody TechnologyCatalogItem item) {
        return ApiResponse.ok(service.addCustom(item));
    }

    @PostMapping("/validate")
    public ApiResponse<TechnologyCatalogService.ValidationReport> validate(@RequestBody ValidateTechRequest req) {
        return ApiResponse.ok(service.validateSelection(
                req.selectedTechnologies(),
                req.mandatoryConstraints(),
                req.forbiddenTechnologies()
        ));
    }
}
