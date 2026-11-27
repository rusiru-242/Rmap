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
      setState(() {
        _catalog = catRes['data'] as List<dynamic>? ?? [];
        _spec = spec;
      });
    } on ApiClientException catch (e) {
      setState(() => _error = e.message);
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

  @override
  Widget build(BuildContext context) {
    final project = ref.watch(projectsControllerProvider).activeProject;
    final theme = Theme.of(context);
    final diagrams = (_spec?['diagrams'] as List<dynamic>? ?? []);

    return ListView(
      padding: const EdgeInsets.all(RmapTokens.space6),
      children: [
        Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Architecture & Technology Catalog', style: theme.textTheme.headlineSmall),
                  Text('Data-driven technology catalog (${_catalog.length} seeded items) & project-specific architecture artifacts.', style: theme.textTheme.bodySmall),
                ],
              ),
            ),
            if (project != null)
              FilledButton(onPressed: _generateArchitecture, child: const Text('Generate Architecture')),
          ],
        ),
        const SizedBox(height: RmapTokens.space4),
        if (_error != null) Text(_error!, style: const TextStyle(color: RmapTokens.danger)),
        if (_spec != null)
          Card(
            child: Padding(
              padding: const EdgeInsets.all(RmapTokens.space4),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Style: ${_spec!['architectureStyle'] ?? 'Not generated yet'}', style: theme.textTheme.titleMedium),
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
    );
  }
}
