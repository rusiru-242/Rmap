import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/network/api_client.dart';
import '../../core/theme/rmap_tokens.dart';
import '../projects/project_state.dart';

class ArchitectureWorkspaceScreen extends ConsumerStatefulWidget {
  const ArchitectureWorkspaceScreen({super.key});

  @override
  ConsumerState<ArchitectureWorkspaceScreen> createState() => _ArchitectureWorkspaceScreenState();
}

class _ArchitectureWorkspaceScreenState extends ConsumerState<ArchitectureWorkspaceScreen> {
  Map<String, dynamic>? _spec;
  List<dynamic> _catalog = [];
  final Set<String> _selectedTech = {};
  Map<String, dynamic>? _validationReport;
  String? _error;

  @override
  void initState() {
    super.initState();
    Future.microtask(_load);
  }

  Future<void> _load() async {
    final project = ref.read(projectsControllerProvider).activeProject;
    try {
      final catRes = await ref.read(apiClientProvider).get('/api/v1/catalog/technologies');
      Map<String, dynamic>? spec;
      if (project != null) {
        final sRes = await ref.read(apiClientProvider).get('/api/v1/projects/${project.id}/specs/latest');
        spec = sRes['data'] as Map<String, dynamic>?;
      }
      if (!mounted) return;
      setState(() {
        _catalog = catRes['data'] as List<dynamic>? ?? [];
        _spec = spec;
      });
    } on ApiClientException catch (e) {
      if (mounted) setState(() => _error = e.message);
    }
  }

  Future<void> _generateArchitecture() async {
    final project = ref.read(projectsControllerProvider).activeProject;
    if (project == null) return;
    try {
      final res = await ref.read(apiClientProvider).post('/api/v1/projects/${project.id}/specs/architecture/generate');
      setState(() => _spec = res['data'] as Map<String, dynamic>?);
    } on ApiClientException catch (e) {
      setState(() => _error = e.message);
    }
  }

  Future<void> _validateTechStack() async {
    final project = ref.read(projectsControllerProvider).activeProject;
    try {
      final res = await ref.read(apiClientProvider).post('/api/v1/catalog/technologies/validate', {
        'selectedTechnologies': _selectedTech.toList(),
        'mandatoryConstraints': project?.mandatoryConstraints ?? <String>[],
        'forbiddenTechnologies': <String>[],
      });
      setState(() {
        _validationReport = res['data'] as Map<String, dynamic>?;
        _error = null;
      });
    } on ApiClientException catch (e) {
      setState(() => _error = e.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    final project = ref.watch(projectsControllerProvider).activeProject;
    final theme = Theme.of(context);
    final diagrams = (_spec?['diagrams'] as List<dynamic>? ?? []);
    final entities = (_spec?['databaseEntities'] as List<dynamic>? ?? []);
    final apis = (_spec?['apiContracts'] as List<dynamic>? ?? []);
    final roles = (_spec?['userRoles'] as List<dynamic>? ?? []);

    return DefaultTabController(
      length: 4,
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(RmapTokens.space6, RmapTokens.space5, RmapTokens.space6, 0),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('Architecture, Stack & Contracts', style: theme.textTheme.headlineSmall),
                      Text(
                        'Data-driven catalog (${_catalog.length} technologies) · Spec-backed architecture, DB, API, and RBAC.',
                        style: theme.textTheme.bodySmall,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ],
                  ),
                ),
                if (project != null)
                  FilledButton(onPressed: _generateArchitecture, child: const Text('Generate Architecture')),
              ],
            ),
          ),
          const SizedBox(height: 8),
          const TabBar(
            isScrollable: true,
            tabs: [
              Tab(text: 'Architecture & Diagrams'),
              Tab(text: 'Technology Stack'),
              Tab(text: 'Database & APIs'),
              Tab(text: 'Roles & Permissions'),
            ],
          ),
          if (_error != null)
            Padding(
              padding: const EdgeInsets.all(8),
              child: Text(_error!, style: const TextStyle(color: RmapTokens.danger)),
            ),
          Expanded(
            child: TabBarView(
              children: [
                ListView(
                  padding: const EdgeInsets.all(RmapTokens.space6),
                  children: [
                    Card(
                      child: Padding(
                        padding: const EdgeInsets.all(RmapTokens.space4),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text('Style: ${_spec?['architectureStyle'] ?? 'Not generated yet'}', style: theme.textTheme.titleMedium),
                            const SizedBox(height: 8),
                            for (final d in diagrams) ...[
                              Text('${d['title']} (${d['diagramType']})', style: theme.textTheme.titleSmall),
                              const SizedBox(height: 4),
                              Container(
                                width: double.infinity,
                                padding: const EdgeInsets.all(10),
                                decoration: BoxDecoration(
                                  color: theme.scaffoldBackgroundColor,
                                  borderRadius: BorderRadius.circular(RmapTokens.radiusSm),
                                  border: Border.all(color: theme.dividerColor),
                                ),
                                child: SelectableText(d['mermaidCode']?.toString() ?? '', style: const TextStyle(fontFamily: 'Consolas', fontSize: 12)),
                              ),
                              const SizedBox(height: 10),
                            ],
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
                ListView(
                  padding: const EdgeInsets.all(RmapTokens.space6),
                  children: [
                    Row(
                      children: [
                        Expanded(child: Text('Select technologies to validate against mandatory project constraints:', style: theme.textTheme.bodySmall)),
                        OutlinedButton(onPressed: _validateTechStack, child: const Text('Validate Constraints')),
                      ],
                    ),
                    if (_validationReport != null) ...[
                      const SizedBox(height: 10),
                      Card(
                        child: Padding(
                          padding: const EdgeInsets.all(12),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                _validationReport!['valid'] == true ? 'Constraint Check: PASSED' : 'Constraint Check: VIOLATIONS FOUND',
                                style: TextStyle(
                                  fontWeight: FontWeight.w600,
                                  color: _validationReport!['valid'] == true ? RmapTokens.success : RmapTokens.danger,
                                ),
                              ),
                              for (final v in (_validationReport!['violations'] as List<dynamic>? ?? []))
                                Text('• $v', style: const TextStyle(color: RmapTokens.danger, fontSize: 12)),
                              for (final w in (_validationReport!['warnings'] as List<dynamic>? ?? []))
                                Text('• $w', style: const TextStyle(color: RmapTokens.warning, fontSize: 12)),
                            ],
                          ),
                        ),
                      ),
                    ],
                    const SizedBox(height: 12),
                    Wrap(
                      spacing: 6,
                      runSpacing: 6,
                      children: _catalog.map((item) {
                        final name = item['name']?.toString() ?? '';
                        return FilterChip(
                          label: Text('$name (${item['category']})'),
                          selected: _selectedTech.contains(name),
                          onSelected: (sel) => setState(() => sel ? _selectedTech.add(name) : _selectedTech.remove(name)),
                        );
                      }).toList(),
                    ),
                  ],
                ),
                ListView(
                  padding: const EdgeInsets.all(RmapTokens.space6),
                  children: [
                    Text('Database Entities (${entities.length})', style: theme.textTheme.titleSmall),
                    const SizedBox(height: 8),
                    if (entities.isEmpty)
                      Text('No database entities defined yet (omitted automatically for standalone ML notebooks).', style: theme.textTheme.bodySmall),
                    for (final e in entities)
                      Card(child: ListTile(title: Text('${e['id']} · ${e['name']}'), subtitle: Text((e['fields'] as List<dynamic>? ?? []).join(', ')))),
                    const SizedBox(height: 16),
                    Text('API Contracts (${apis.length})', style: theme.textTheme.titleSmall),
                    const SizedBox(height: 8),
                    if (apis.isEmpty)
                      Text('No REST API contracts defined yet.', style: theme.textTheme.bodySmall),
                    for (final a in apis)
                      Card(child: ListTile(title: Text('${a['method']} ${a['path']}'), subtitle: Text(a['summary']?.toString() ?? ''))),
                  ],
                ),
                ListView(
                  padding: const EdgeInsets.all(RmapTokens.space6),
                  children: [
                    Text('User Roles & Permissions (${roles.length})', style: theme.textTheme.titleSmall),
                    const SizedBox(height: 8),
                    if (roles.isEmpty)
                      Text('Run the Architect Agent or edit ProjectSpec to populate roles & permissions.', style: theme.textTheme.bodySmall),
                    for (final r in roles)
                      Card(child: ListTile(title: Text('${r['id']} · ${r['roleName']}'), subtitle: Text((r['permissions'] as List<dynamic>? ?? []).join(', ')))),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
