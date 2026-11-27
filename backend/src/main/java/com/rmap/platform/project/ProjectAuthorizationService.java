package com.rmap.platform.project;

import com.rmap.platform.auth.AuthenticatedUser;
import com.rmap.platform.common.ApiException;
import com.rmap.platform.user.Role;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProjectAuthorizationService {
    private final ProjectRepository projectRepository;

    public ProjectAuthorizationService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public Project requireMember(String projectId, AuthenticatedUser user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found."));
        if (user.role() == Role.ADMIN || isMember(project, user.userId())) {
            return project;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "PROJECT_ACCESS_DENIED", "You do not have access to this project.");
    }

    public Project requireEditor(String projectId, AuthenticatedUser user) {
        Project project = requireMember(projectId, user);
        if (user.role() == Role.ADMIN || project.getOwnerId().equals(user.userId())) {
            return project;
        }
        Optional<Project.ProjectMember> member = project.getMembers().stream()
                .filter(m -> m.userId().equals(user.userId()))
                .findFirst();
        if (member.isPresent() && (member.get().role() == ProjectRole.OWNER || member.get().role() == ProjectRole.EDITOR)) {
            return project;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "PROJECT_WRITE_DENIED", "Editor or Owner role required for this action.");
    }

    public Project requireOwner(String projectId, AuthenticatedUser user) {
        Project project = requireMember(projectId, user);
        if (user.role() == Role.ADMIN || project.getOwnerId().equals(user.userId())) {
            return project;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "PROJECT_OWNER_REQUIRED", "Only the project owner can perform this action.");
    }

    private boolean isMember(Project project, String userId) {
        if (project.getOwnerId().equals(userId)) {
            return true;
        }
        return project.getMembers().stream().anyMatch(m -> m.userId().equals(userId));
    }
}
