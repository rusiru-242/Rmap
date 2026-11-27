import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/navigation/nav_registry.dart';
import '../../core/network/api_client.dart';

class ProjectSummary {
  final String id;
  final String name;
  final String description;
  final String mode;
  final String status;
  final List<String> archetypes;
  final List<String> mandatoryConstraints;
  final int teamSize;
  final int activeSpecVersion;

  const ProjectSummary({
    required this.id,
    required this.name,
    required this.description,
    required this.mode,
    required this.status,
    required this.archetypes,
    required this.mandatoryConstraints,
    required this.teamSize,
    required this.activeSpecVersion,
  });

  ProjectContextMode toContextMode() {
    switch (mode.toUpperCase()) {
      case 'ACADEMIC':
        return ProjectContextMode.academic;
      case 'TEAM':
        return ProjectContextMode.team;
      case 'CLIENT':
        return ProjectContextMode.client;
      default:
        if (archetypes.any((a) => a.toLowerCase().contains('machine learning') || a.toLowerCase().contains('data science'))) {
          return ProjectContextMode.ml;
        }
        return ProjectContextMode.general;
    }
  }

  factory ProjectSummary.fromJson(Map<String, dynamic> json) {
    return ProjectSummary(
      id: json['id']?.toString() ?? '',
      name: json['name']?.toString() ?? 'Untitled',
      description: json['description']?.toString() ?? '',
      mode: json['mode']?.toString() ?? 'INDIVIDUAL',
      status: json['status']?.toString() ?? 'ACTIVE',
      archetypes: (json['archetypes'] as List<dynamic>? ?? []).map((e) => e.toString()).toList(),
      mandatoryConstraints: (json['mandatoryConstraints'] as List<dynamic>? ?? []).map((e) => e.toString()).toList(),
      teamSize: (json['teamSize'] as num?)?.toInt() ?? 1,
      activeSpecVersion: (json['activeSpecVersion'] as num?)?.toInt() ?? 0,
    );
  }
}

class ProjectsState {
  final bool loading;
  final List<ProjectSummary> projects;
  final ProjectSummary? activeProject;
  final String? error;

  const ProjectsState({
    this.loading = false,
    this.projects = const [],
    this.activeProject,
    this.error,
  });

  ProjectsState copyWith({
    bool? loading,
    List<ProjectSummary>? projects,
    ProjectSummary? activeProject,
    String? error,
    bool clearError = false,
  }) {
    return ProjectsState(
      loading: loading ?? this.loading,
      projects: projects ?? this.projects,
      activeProject: activeProject ?? this.activeProject,
      error: clearError ? null : (error ?? this.error),
    );
  }
}

class ProjectsController extends StateNotifier<ProjectsState> {
  final RmapApiClient _api;

  ProjectsController(this._api) : super(const ProjectsState());

  Future<void> refresh({String? query, String? mode, String? status}) async {
    state = state.copyWith(loading: true, clearError: true);
    try {
      final res = await _api.get('/api/v1/projects', query: {
        if (query != null && query.isNotEmpty) 'q': query,
        if (mode != null && mode.isNotEmpty) 'mode': mode,
        if (status != null && status.isNotEmpty) 'status': status,
      });
      final items = (res['data'] as List<dynamic>? ?? [])
          .map((e) => ProjectSummary.fromJson(e as Map<String, dynamic>))
          .toList();
      final currentId = state.activeProject?.id;
      final selected = items.where((p) => p.id == currentId).firstOrNull ?? items.firstOrNull;
      state = state.copyWith(loading: false, projects: items, activeProject: selected);
    } on ApiClientException catch (e) {
      state = state.copyWith(loading: false, error: e.message);
    }
  }

  void selectProject(ProjectSummary project) {
    state = state.copyWith(activeProject: project);
  }

  Future<ProjectSummary?> createProject(Map<String, dynamic> payload) async {
    state = state.copyWith(loading: true, clearError: true);
    try {
      final res = await _api.post('/api/v1/projects', payload);
      final created = ProjectSummary.fromJson(res['data'] as Map<String, dynamic>);
      state = state.copyWith(
        loading: false,
        projects: [created, ...state.projects],
        activeProject: created,
      );
      return created;
    } on ApiClientException catch (e) {
      state = state.copyWith(loading: false, error: e.message);
      return null;
    }
  }

  Future<void> duplicate(String projectId) async {
    try {
      await _api.post('/api/v1/projects/$projectId/duplicate');
      await refresh();
    } on ApiClientException catch (e) {
      state = state.copyWith(error: e.message);
    }
  }

  Future<void> toggleArchive(ProjectSummary project) async {
    final action = project.status == 'ARCHIVED' ? 'restore' : 'archive';
    try {
      await _api.post('/api/v1/projects/${project.id}/$action');
      await refresh();
    } on ApiClientException catch (e) {
      state = state.copyWith(error: e.message);
    }
  }
}

final projectsControllerProvider = StateNotifierProvider<ProjectsController, ProjectsState>((ref) {
  return ProjectsController(ref.watch(apiClientProvider));
});
