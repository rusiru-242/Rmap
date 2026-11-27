package com.rmap.platform.project;

import com.rmap.platform.auth.AuthenticatedUser;
import com.rmap.platform.common.ApiException;
import com.rmap.platform.common.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectAuthorizationService authService;
    private final AuditService auditService;
    private final Clock clock;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectAuthorizationService authService,
            AuditService auditService,
            Clock clock
    ) {
        this.projectRepository = projectRepository;
        this.authService = authService;
        this.auditService = auditService;
        this.clock = clock;
    }

    public Project create(AuthenticatedUser user, ProjectDtos.CreateProjectRequest req) {
        Instant now = clock.instant();
        Project p = new Project();
        p.setOwnerId(user.userId());
        p.setName(req.name().trim());
        p.setDescription(safeTrim(req.description()));
        p.setProblemStatement(safeTrim(req.problemStatement()));
        p.setTargetUsers(safeTrim(req.targetUsers()));
        p.setBusinessObjectives(copyList(req.businessObjectives()));
        p.setMode(req.mode());
        p.setArchetypes(copyList(req.archetypes()));
        p.setPlatforms(copyList(req.platforms()));
        p.setTeamSize(req.teamSize() == null ? 1 : req.teamSize());
        p.setExperienceLevel(safeTrim(req.experienceLevel()));
        p.setBudget(safeTrim(req.budget()));
        p.setDeadline(safeTrim(req.deadline()));
        p.setTechnologyPreferences(copyList(req.technologyPreferences()));
        p.setMandatoryConstraints(copyList(req.mandatoryConstraints()));
        p.setDeliverables(copyList(req.deliverables()));
        p.setInputSources(copyList(req.inputSources()));
        p.setLocalWorkspaceId(safeTrim(req.localWorkspaceId()));
        p.setStatus(ProjectStatus.ACTIVE);
        p.setMembers(new ArrayList<>(List.of(
                new Project.ProjectMember(user.userId(), user.email(), ProjectRole.OWNER, "Project Owner", now)
        )));
        p.setActivityHistory(new ArrayList<>(List.of(
                new Project.ProjectActivity(user.userId(), user.email(), "PROJECT_CREATED", "Created project " + p.getName(), now)
        )));

        p = projectRepository.save(p);
        auditService.record(user.userId(), user.email(), "PROJECT_CREATED", "PROJECT", p.getId(), Map.of("mode", p.getMode().name()));
        return p;
    }

    public List<Project> list(AuthenticatedUser user, String query, ProjectMode mode, ProjectStatus status) {
        return projectRepository.findAccessibleByUserId(user.userId()).stream()
                .filter(p -> status == null || p.getStatus() == status)
                .filter(p -> mode == null || p.getMode() == mode)
                .filter(p -> query == null || query.isBlank()
                        || p.getName().toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT))
                        || (p.getDescription() != null && p.getDescription().toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT)))
                        || p.getArchetypes().stream().anyMatch(a -> a.toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT))))
                .sorted(Comparator.comparing(Project::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public Project get(String projectId, AuthenticatedUser user) {
        return authService.requireMember(projectId, user);
    }

    public Project update(String projectId, AuthenticatedUser user, ProjectDtos.UpdateProjectRequest req) {
        Project p = authService.requireEditor(projectId, user);
        if (req.name() != null && !req.name().isBlank()) p.setName(req.name().trim());
        if (req.description() != null) p.setDescription(req.description().trim());
        if (req.problemStatement() != null) p.setProblemStatement(req.problemStatement().trim());
        if (req.targetUsers() != null) p.setTargetUsers(req.targetUsers().trim());
        if (req.businessObjectives() != null) p.setBusinessObjectives(copyList(req.businessObjectives()));
        if (req.mode() != null) p.setMode(req.mode());
        if (req.archetypes() != null) p.setArchetypes(copyList(req.archetypes()));
        if (req.platforms() != null) p.setPlatforms(copyList(req.platforms()));
        if (req.teamSize() != null) p.setTeamSize(req.teamSize());
        if (req.experienceLevel() != null) p.setExperienceLevel(req.experienceLevel().trim());
        if (req.budget() != null) p.setBudget(req.budget().trim());
        if (req.deadline() != null) p.setDeadline(req.deadline().trim());
        if (req.technologyPreferences() != null) p.setTechnologyPreferences(copyList(req.technologyPreferences()));
        if (req.mandatoryConstraints() != null) p.setMandatoryConstraints(copyList(req.mandatoryConstraints()));
        if (req.deliverables() != null) p.setDeliverables(copyList(req.deliverables()));
        if (req.localWorkspaceId() != null) p.setLocalWorkspaceId(req.localWorkspaceId().trim());

        appendActivity(p, user, "PROJECT_UPDATED", "Updated project metadata");
        return projectRepository.save(p);
    }

    public Project duplicate(String projectId, AuthenticatedUser user) {
        Project src = authService.requireMember(projectId, user);
        Instant now = clock.instant();
        Project copy = new Project();
        copy.setOwnerId(user.userId());
        copy.setName(src.getName() + " (Copy)");
        copy.setDescription(src.getDescription());
        copy.setProblemStatement(src.getProblemStatement());
        copy.setTargetUsers(src.getTargetUsers());
        copy.setBusinessObjectives(new ArrayList<>(src.getBusinessObjectives()));
        copy.setMode(src.getMode());
        copy.setArchetypes(new ArrayList<>(src.getArchetypes()));
        copy.setPlatforms(new ArrayList<>(src.getPlatforms()));
        copy.setTeamSize(src.getTeamSize());
        copy.setExperienceLevel(src.getExperienceLevel());
        copy.setBudget(src.getBudget());
        copy.setDeadline(src.getDeadline());
        copy.setTechnologyPreferences(new ArrayList<>(src.getTechnologyPreferences()));
        copy.setMandatoryConstraints(new ArrayList<>(src.getMandatoryConstraints()));
        copy.setDeliverables(new ArrayList<>(src.getDeliverables()));
        copy.setInputSources(new ArrayList<>(src.getInputSources()));
        copy.setStatus(ProjectStatus.ACTIVE);
        copy.setMembers(new ArrayList<>(List.of(
                new Project.ProjectMember(user.userId(), user.email(), ProjectRole.OWNER, "Project Owner", now)
        )));
        copy.setActivityHistory(new ArrayList<>(List.of(
                new Project.ProjectActivity(user.userId(), user.email(), "PROJECT_DUPLICATED", "Duplicated from " + src.getId(), now)
        )));
        return projectRepository.save(copy);
    }

    public Project setArchiveState(String projectId, AuthenticatedUser user, boolean archived) {
        Project p = authService.requireOwner(projectId, user);
        p.setStatus(archived ? ProjectStatus.ARCHIVED : ProjectStatus.ACTIVE);
        appendActivity(p, user, archived ? "PROJECT_ARCHIVED" : "PROJECT_RESTORED", archived ? "Archived project" : "Restored project");
        return projectRepository.save(p);
    }

    public void delete(String projectId, AuthenticatedUser user, String confirmName) {
        Project p = authService.requireOwner(projectId, user);
        if (confirmName == null || !p.getName().equals(confirmName.trim())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DELETE_CONFIRMATION_REQUIRED", "Confirmation must match the exact project name.");
        }
        projectRepository.delete(p);
        auditService.record(user.userId(), user.email(), "PROJECT_DELETED", "PROJECT", projectId, Map.of("name", p.getName()));
    }

    public Project inviteMember(String projectId, AuthenticatedUser user, ProjectDtos.InviteMemberRequest req) {
        Project p = authService.requireOwner(projectId, user);
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        var inv = new Project.ProjectInvitation(
                UUID.randomUUID().toString(),
                email,
                req.role(),
                safeTrim(req.responsibility()),
                "PENDING",
                clock.instant()
        );
        p.getInvitations().add(inv);
        appendActivity(p, user, "MEMBER_INVITED", "Invited " + email + " as " + req.role().name());
        return projectRepository.save(p);
    }

    public Project linkLocalWorkspace(String projectId, AuthenticatedUser user, String localWorkspaceId) {
        Project p = authService.requireEditor(projectId, user);
        p.setLocalWorkspaceId(localWorkspaceId.trim());
        appendActivity(p, user, "WORKSPACE_LINKED", "Associated local workspace " + localWorkspaceId.trim());
        return projectRepository.save(p);
    }

    private void appendActivity(Project p, AuthenticatedUser user, String action, String detail) {
        p.getActivityHistory().add(new Project.ProjectActivity(user.userId(), user.email(), action, detail, clock.instant()));
        if (p.getActivityHistory().size() > 200) {
            p.setActivityHistory(new ArrayList<>(p.getActivityHistory().subList(p.getActivityHistory().size() - 200, p.getActivityHistory().size())));
        }
    }

    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private List<String> copyList(List<String> input) {
        return input == null ? new ArrayList<>() : new ArrayList<>(input.stream().filter(v -> v != null && !v.isBlank()).map(String::trim).toList());
    }
}
