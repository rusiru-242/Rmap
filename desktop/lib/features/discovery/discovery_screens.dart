import 'package:file_selector/file_selector.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path/path.dart' as p;
import '../../core/network/api_client.dart';
import '../../core/theme/rmap_tokens.dart';
import '../projects/project_state.dart';

class RequirementsWorkspaceScreen extends ConsumerStatefulWidget {
  const RequirementsWorkspaceScreen({super.key});

  @override
  ConsumerState<RequirementsWorkspaceScreen> createState() => _RequirementsWorkspaceScreenState();
}

class _RequirementsWorkspaceScreenState extends ConsumerState<RequirementsWorkspaceScreen> {
  Map<String, dynamic>? _spec;
  bool _loading = false;
  String? _error;
  String? _info;
  final _reqTitleCtrl = TextEditingController();
  final _reqDescCtrl = TextEditingController();

  @override
  void initState() {
    super.initState();
    Future.microtask(_loadSpec);
  }

  @override
  void dispose() {
    _reqTitleCtrl.dispose();
    _reqDescCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadSpec() async {
    final project = ref.read(projectsControllerProvider).activeProject;
    if (project == null) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final res = await ref.read(apiClientProvider).get('/api/v1/projects/${project.id}/specs/latest');
      setState(() {
        _spec = res['data'] as Map<String, dynamic>?;
        _loading = false;
      });
    } on ApiClientException catch (e) {
      setState(() {
        _error = e.message;
        _loading = false;
      });
    }
  }

  Future<void> _addRequirement() async {
    final project = ref.read(projectsControllerProvider).activeProject;
    if (project == null || _spec == null || _reqTitleCtrl.text.trim().isEmpty) return;
    final list = List<Map<String, dynamic>>.from(
      (_spec!['functionalRequirements'] as List<dynamic>? ?? []).map((e) => Map<String, dynamic>.from(e as Map)),
    );
    final nextId = 'REQ-${(list.length + 1).toString().padLeft(3, '0')}';
    list.add({
      'id': nextId,
      'title': _reqTitleCtrl.text.trim(),
      'description': _reqDescCtrl.text.trim(),
      'priority': 'HIGH',
      'sourceRef': 'Manual Entry',
      'aiRecommended': false,
      'confirmed': true,
      'dependsOn': <String>[],
    });
    final updated = Map<String, dynamic>.from(_spec!)..['functionalRequirements'] = list;
    try {
      final res = await ref.read(apiClientProvider).put('/api/v1/projects/${project.id}/specs/draft', updated);
      _reqTitleCtrl.clear();
      _reqDescCtrl.clear();
      setState(() {
        _spec = res['data'] as Map<String, dynamic>;
        _info = 'Saved $nextId to Spec v${_spec!['versionNumber']} (${_spec!['status']}).';
      });
    } on ApiClientException catch (e) {
      setState(() => _error = e.message);
    }
  }

  Future<void> _approveSpec() async {
    final project = ref.read(projectsControllerProvider).activeProject;
    if (project == null || _spec == null) return;
    final v = _spec!['versionNumber'];
    try {
      final res = await ref.read(apiClientProvider).post('/api/v1/projects/${project.id}/specs/$v/approve');
      setState(() {
        _spec = res['data'] as Map<String, dynamic>;
        _info = 'Approved ProjectSpec v$v. Future edits will fork a new draft version.';
      });
    } on ApiClientException catch (e) {
      setState(() => _error = e.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    final project = ref.watch(projectsControllerProvider).activeProject;
    final theme = Theme.of(context);
    if (project == null) {
      return const Center(child: Text('Select or create a project in My Projects first.'));
    }

    final reqs = (_spec?['functionalRequirements'] as List<dynamic>? ?? []);
    return ListView(
      padding: const EdgeInsets.all(RmapTokens.space6),
      children: [
        Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('ProjectSpec & Requirements (${project.name})', style: theme.textTheme.headlineSmall),
                  Text(
                    _spec == null ? 'Loading...' : 'Version v${_spec!['versionNumber']} · Status: ${_spec!['status']}',
                    style: theme.textTheme.bodySmall,
                  ),
                ],
              ),
            ),
            if (_spec != null && _spec!['status'] != 'APPROVED')
              FilledButton(onPressed: _approveSpec, child: const Text('Approve Spec')),
          ],
        ),
        const SizedBox(height: RmapTokens.space4),
        if (_error != null) Text(_error!, style: const TextStyle(color: RmapTokens.danger)),
        if (_info != null) Text(_info!, style: const TextStyle(color: RmapTokens.success)),
        if (_loading) const LinearProgressIndicator(),
        const SizedBox(height: RmapTokens.space3),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(RmapTokens.space4),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Add Functional Requirement', style: theme.textTheme.titleSmall),
                const SizedBox(height: 8),
                TextField(controller: _reqTitleCtrl, decoration: const InputDecoration(hintText: 'Requirement title')),
                const SizedBox(height: 8),
                TextField(controller: _reqDescCtrl, decoration: const InputDecoration(hintText: 'Acceptance details')),
                const SizedBox(height: 8),
                OutlinedButton(onPressed: _addRequirement, child: const Text('Add Requirement')),
              ],
            ),
          ),
        ),
        const SizedBox(height: RmapTokens.space4),
        for (final r in reqs)
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: Card(
              child: ListTile(
                title: Text('${r['id']} · ${r['title']}', style: theme.textTheme.titleSmall),
                subtitle: Text('${r['description'] ?? ""}\nSource: ${r['sourceRef'] ?? "N/A"}'),
                trailing: Text(r['confirmed'] == true ? 'Confirmed' : 'AI Suggested', style: theme.textTheme.labelSmall),
              ),
            ),
          ),
      ],
    );
  }
}

class DocumentsScreen extends ConsumerStatefulWidget {
  const DocumentsScreen({super.key});

  @override
  ConsumerState<DocumentsScreen> createState() => _DocumentsScreenState();
}

class _DocumentsScreenState extends ConsumerState<DocumentsScreen> {
  List<dynamic> _docs = [];
  bool _loading = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    Future.microtask(_load);
  }

  Future<void> _load() async {
    final project = ref.read(projectsControllerProvider).activeProject;
    if (project == null) return;
    setState(() => _loading = true);
    try {
      final res = await ref.read(apiClientProvider).get('/api/v1/projects/${project.id}/documents');
      setState(() {
        _docs = res['data'] as List<dynamic>? ?? [];
        _loading = false;
      });
    } on ApiClientException catch (e) {
      setState(() {
        _error = e.message;
        _loading = false;
      });
    }
  }

  Future<void> _uploadFile() async {
    final project = ref.read(projectsControllerProvider).activeProject;
    if (project == null) return;
    final file = await openFile();
    if (file == null) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      await ref.read(apiClientProvider).postMultipart(
            '/api/v1/projects/${project.id}/documents',
            filePath: file.path,
            fileName: p.basename(file.path),
          );
      await _load();
    } on ApiClientException catch (e) {
      setState(() {
        _error = e.message;
        _loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final project = ref.watch(projectsControllerProvider).activeProject;
    final theme = Theme.of(context);
    if (project == null) return const Center(child: Text('Select a project first.'));

    return ListView(
      padding: const EdgeInsets.all(RmapTokens.space6),
      children: [
        Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Document Intelligence', style: theme.textTheme.headlineSmall),
                  Text('Safe PDF, DOCX, XLSX, CSV, TXT, MD, Image, and ZIP parsing with secret redaction.', style: theme.textTheme.bodySmall),
                ],
              ),
            ),
            FilledButton.icon(
              onPressed: _loading ? null : _uploadFile,
              icon: const Icon(Icons.upload_file_outlined, size: 16),
              label: const Text('Upload Document'),
            ),
          ],
        ),
        const SizedBox(height: RmapTokens.space4),
        if (_error != null) Text(_error!, style: const TextStyle(color: RmapTokens.danger)),
        if (_loading) const LinearProgressIndicator(),
        for (final d in _docs)
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: Card(
              child: ListTile(
                title: Text('${d['originalFileName']} (v${d['versionNumber']})', style: theme.textTheme.titleSmall),
                subtitle: Text(d['extractedSummary']?.toString() ?? ''),
                trailing: Text(d['detectedType']?.toString() ?? '', style: theme.textTheme.labelSmall),
              ),
            ),
          ),
      ],
    );
  }
}
