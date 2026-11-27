import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/network/api_client.dart';
import '../../core/theme/rmap_tokens.dart';
import '../projects/project_state.dart';

class UiUxDesignStudioScreen extends ConsumerStatefulWidget {
  const UiUxDesignStudioScreen({super.key});

  @override
  ConsumerState<UiUxDesignStudioScreen> createState() => _UiUxDesignStudioScreenState();
}

class _UiUxDesignStudioScreenState extends ConsumerState<UiUxDesignStudioScreen> {
  String _surface = 'WEB';
  List<dynamic> _artifacts = [];
  String? _error;

  @override
  void initState() {
    super.initState();
    Future.microtask(_load);
  }

  Future<void> _load() async {
    final project = ref.read(projectsControllerProvider).activeProject;
    if (project == null) return;
    try {
      final res = await ref.read(apiClientProvider).get('/api/v1/projects/${project.id}/delivery/design');
      if (mounted) setState(() => _artifacts = res['data'] as List<dynamic>? ?? []);
    } on ApiClientException catch (e) {
      if (mounted) setState(() => _error = e.message);
    }
  }

  Future<void> _generate() async {
    final project = ref.read(projectsControllerProvider).activeProject;
    if (project == null) return;
    try {
      await ref.read(apiClientProvider).post('/api/v1/projects/${project.id}/delivery/design/stitch-prompt', {
        'targetSurface': _surface,
        'visualNotes': 'Linear / VS Code neutral developer aesthetic',
      });
      await _load();
    } on ApiClientException catch (e) {
      setState(() => _error = e.message);
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
                  Text('UI/UX Design Studio & Google Stitch Prompts', style: theme.textTheme.headlineSmall),
                  Text('Guided Stitch prompt generation and design integration preserving real APIs and auth.', style: theme.textTheme.bodySmall),
                ],
              ),
            ),
            DropdownButton<String>(
              value: _surface,
              items: const ['WEB', 'DESKTOP', 'MOBILE', 'ADMIN', 'LANDING'].map((s) => DropdownMenuItem(value: s, child: Text(s))).toList(),
              onChanged: (v) => setState(() => _surface = v ?? 'WEB'),
            ),
            const SizedBox(width: 8),
            FilledButton(onPressed: _generate, child: const Text('Generate Stitch Prompt')),
          ],
        ),
        if (_error != null) Text(_error!, style: const TextStyle(color: RmapTokens.danger)),
        const SizedBox(height: RmapTokens.space4),
        for (final a in _artifacts)
          Padding(
            padding: const EdgeInsets.only(bottom: 10),
            child: Card(
              child: Padding(
                padding: const EdgeInsets.all(RmapTokens.space4),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(a['title']?.toString() ?? '', style: theme.textTheme.titleSmall),
                    const SizedBox(height: 6),
                    SelectableText(a['stitchPrompt']?.toString() ?? '', style: const TextStyle(fontFamily: 'Consolas', fontSize: 12)),
                  ],
                ),
              ),
            ),
          ),
      ],
    );
  }
}

class DocumentationScreen extends ConsumerStatefulWidget {
  const DocumentationScreen({super.key});

  @override
  ConsumerState<DocumentationScreen> createState() => _DocumentationScreenState();
}

class _DocumentationScreenState extends ConsumerState<DocumentationScreen> {
  String _docType = 'SRS';
  List<dynamic> _docs = [];
  String? _error;

  @override
  void initState() {
    super.initState();
    Future.microtask(_load);
  }

  Future<void> _load() async {
    final project = ref.read(projectsControllerProvider).activeProject;
    if (project == null) return;
    try {
      final res = await ref.read(apiClientProvider).get('/api/v1/projects/${project.id}/delivery/docs');
      if (mounted) setState(() => _docs = res['data'] as List<dynamic>? ?? []);
    } on ApiClientException catch (e) {
      if (mounted) setState(() => _error = e.message);
    }
  }

  Future<void> _generate() async {
    final project = ref.read(projectsControllerProvider).activeProject;
    if (project == null) return;
    try {
      await ref.read(apiClientProvider).post('/api/v1/projects/${project.id}/delivery/docs', {'docType': _docType});
      await _load();
    } on ApiClientException catch (e) {
      setState(() => _error = e.message);
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
                  Text('Specification-Driven Documentation Engine', style: theme.textTheme.headlineSmall),
                  Text('Exports real Markdown, PDF, and DOCX artifacts referencing approved ProjectSpec versions.', style: theme.textTheme.bodySmall),
                ],
              ),
            ),
            DropdownButton<String>(
              value: _docType,
              items: const ['SRS', 'SDD', 'ADR', 'API_DOCS', 'DB_DOCS', 'README', 'TEST_PLAN', 'SECURITY_DOC', 'CLIENT_HANDOVER', 'DEPLOYMENT_GUIDE']
                  .map((t) => DropdownMenuItem(value: t, child: Text(t)))
                  .toList(),
              onChanged: (v) => setState(() => _docType = v ?? 'SRS'),
            ),
            const SizedBox(width: 8),
            FilledButton(onPressed: _generate, child: const Text('Generate Doc')),
          ],
        ),
        if (_error != null) Text(_error!, style: const TextStyle(color: RmapTokens.danger)),
        const SizedBox(height: RmapTokens.space4),
        for (final d in _docs)
          Padding(
            padding: const EdgeInsets.only(bottom: 10),
            child: Card(
              child: Padding(
                padding: const EdgeInsets.all(RmapTokens.space4),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('${d['title']} (v${d['versionNumber']} · Spec v${d['specVersion']})', style: theme.textTheme.titleSmall),
                    const SizedBox(height: 6),
                    SelectableText(d['markdownContent']?.toString() ?? '', style: const TextStyle(fontFamily: 'Consolas', fontSize: 11.5)),
                  ],
                ),
              ),
            ),
          ),
      ],
    );
  }
}

class DeploymentCenterScreen extends ConsumerStatefulWidget {
  const DeploymentCenterScreen({super.key});

  @override
  ConsumerState<DeploymentCenterScreen> createState() => _DeploymentCenterScreenState();
}

class _DeploymentCenterScreenState extends ConsumerState<DeploymentCenterScreen> {
  String _provider = 'Render';
  List<dynamic> _plans = [];
  String? _error;

  @override
  void initState() {
    super.initState();
    Future.microtask(_load);
  }

  Future<void> _load() async {
    final project = ref.read(projectsControllerProvider).activeProject;
    if (project == null) return;
    try {
      final res = await ref.read(apiClientProvider).get('/api/v1/projects/${project.id}/delivery/deployments');
      if (mounted) setState(() => _plans = res['data'] as List<dynamic>? ?? []);
    } on ApiClientException catch (e) {
      if (mounted) setState(() => _error = e.message);
    }
  }

  Future<void> _generate() async {
    final project = ref.read(projectsControllerProvider).activeProject;
    if (project == null) return;
    try {
      await ref.read(apiClientProvider).post('/api/v1/projects/${project.id}/delivery/deployments', {
        'provider': _provider,
        'targetType': 'WEB_SERVICE',
        'rootDirectory': '.',
        'buildCommand': 'npm run build',
        'startCommand': 'npm start',
      });
      await _load();
    } on ApiClientException catch (e) {
      setState(() => _error = e.message);
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
                  Text('Deployment Center', style: theme.textTheme.headlineSmall),
                  Text('Beginner-friendly 20-step click-by-click manual deployment guides. Never publishes without explicit authorization.', style: theme.textTheme.bodySmall),
                ],
              ),
            ),
            DropdownButton<String>(
              value: _provider,
              items: const ['Render', 'Vercel', 'Cloudflare Pages', 'Netlify', 'Railway', 'AWS / Azure / Google Cloud']
                  .map((p) => DropdownMenuItem(value: p, child: Text(p)))
                  .toList(),
              onChanged: (v) => setState(() => _provider = v ?? 'Render'),
            ),
            const SizedBox(width: 8),
            FilledButton(onPressed: _generate, child: const Text('Generate 20-Step Guide')),
          ],
        ),
        if (_error != null) Text(_error!, style: const TextStyle(color: RmapTokens.danger)),
        const SizedBox(height: RmapTokens.space4),
        for (final p in _plans)
          Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: Card(
              child: Padding(
                padding: const EdgeInsets.all(RmapTokens.space4),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('${p['provider']} (${p['verificationStatus']})', style: theme.textTheme.titleMedium),
                    const SizedBox(height: 4),
                    Text(p['providerGuidanceNotice']?.toString() ?? '', style: const TextStyle(color: RmapTokens.warning, fontSize: 12)),
                    const SizedBox(height: 8),
                    for (final step in (p['clickByClickManualSteps'] as List<dynamic>? ?? []))
                      Padding(padding: const EdgeInsets.only(bottom: 3), child: Text(step.toString(), style: theme.textTheme.bodySmall)),
                  ],
                ),
              ),
            ),
          ),
      ],
    );
  }
}

class TestingSecurityAndDeliveryScreen extends ConsumerStatefulWidget {
  final String modeTitle;
  const TestingSecurityAndDeliveryScreen({super.key, required this.modeTitle});

  @override
  ConsumerState<TestingSecurityAndDeliveryScreen> createState() => _TestingSecurityAndDeliveryScreenState();
}

class _TestingSecurityAndDeliveryScreenState extends ConsumerState<TestingSecurityAndDeliveryScreen> {
  Map<String, dynamic>? _guidance;
  Map<String, dynamic>? _academicPack;
  String? _error;

  @override
  void initState() {
    super.initState();
    Future.microtask(_load);
  }

  Future<void> _load() async {
    final project = ref.read(projectsControllerProvider).activeProject;
    if (project == null) return;
    try {
      final ts = await ref.read(apiClientProvider).get('/api/v1/projects/${project.id}/delivery/testing-security');
      final ap = await ref.read(apiClientProvider).get('/api/v1/projects/${project.id}/delivery/academic-pack');
      if (mounted) {
        setState(() {
          _guidance = ts['data'] as Map<String, dynamic>?;
          _academicPack = ap['data'] as Map<String, dynamic>?;
        });
      }
    } on ApiClientException catch (e) {
      if (mounted) setState(() => _error = e.message);
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
        Text(widget.modeTitle, style: theme.textTheme.headlineSmall),
        const SizedBox(height: RmapTokens.space4),
        if (_error != null) Text(_error!, style: const TextStyle(color: RmapTokens.danger)),
        if (_guidance != null)
          Card(
            child: Padding(
              padding: const EdgeInsets.all(RmapTokens.space4),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Defensive Security & Testing Checklist', style: theme.textTheme.titleSmall),
                  const SizedBox(height: 6),
                  for (final item in (_guidance!['securityChecklist'] as List<dynamic>? ?? []))
                    Text('• $item', style: theme.textTheme.bodySmall),
                  const SizedBox(height: 8),
                  Text(_guidance!['disclaimer']?.toString() ?? '', style: const TextStyle(color: RmapTokens.warning, fontSize: 12)),
                ],
              ),
            ),
          ),
        if (_academicPack != null) ...[
          const SizedBox(height: 12),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(RmapTokens.space4),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Viva Preparation & Delivery Checklist (AI Usage Logs: ${_academicPack!['aiUsageLogCount']})', style: theme.textTheme.titleSmall),
                  const SizedBox(height: 6),
                  for (final q in (_academicPack!['vivaPreparationQuestions'] as List<dynamic>? ?? []))
                    Text('• $q', style: theme.textTheme.bodySmall),
                ],
              ),
            ),
          ),
        ],
      ],
    );
  }
}
