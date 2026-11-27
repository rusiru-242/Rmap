package com.rmap.platform.project;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class ProjectDtos {
    private ProjectDtos() {}

    public record CreateProjectRequest(
            @NotBlank @Size(min = 2, max = 140) String name,
            @Size(max = 4000) String description,
            @Size(max = 4000) String problemStatement,
            @Size(max = 1000) String targetUsers,
            List<String> businessObjectives,
            @NotNull ProjectMode mode,
            List<String> archetypes,
            List<String> platforms,
            @Min(1) @Max(200) Integer teamSize,
            @Size(max = 100) String experienceLevel,
            @Size(max = 200) String budget,
            @Size(max = 120) String deadline,
            List<String> technologyPreferences,
            List<String> mandatoryConstraints,
            List<String> deliverables,
            List<String> inputSources,
            String localWorkspaceId
    ) {}

    public record UpdateProjectRequest(
            @Size(min = 2, max = 140) String name,
            @Size(max = 4000) String description,
            @Size(max = 4000) String problemStatement,
            @Size(max = 1000) String targetUsers,
            List<String> businessObjectives,
            ProjectMode mode,
            List<String> archetypes,
            List<String> platforms,
            @Min(1) @Max(200) Integer teamSize,
            @Size(max = 100) String experienceLevel,
            @Size(max = 200) String budget,
            @Size(max = 120) String deadline,
            List<String> technologyPreferences,
            List<String> mandatoryConstraints,
            List<String> deliverables,
            String localWorkspaceId
    ) {}

    public record InviteMemberRequest(
            @NotBlank @Email String email,
            @NotNull ProjectRole role,
            @Size(max = 200) String responsibility
    ) {}

    public record LinkWorkspaceRequest(
            @NotBlank String localWorkspaceId
    ) {}
}
