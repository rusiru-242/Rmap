import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/network/api_client.dart';
import '../../core/theme/rmap_tokens.dart';
import '../projects/project_state.dart';
import '../workspace/local_execution_engine.dart';
import '../workspace/local_workspace_screen.dart';

class EnvironmentSetupScreen extends ConsumerStatefulWidget {
  const EnvironmentSetupScreen({super.key});

  @override
  ConsumerState<EnvironmentSetupScreen> createState() => _EnvironmentSetupScreenState();
}

class _EnvironmentSetupScreenState extends ConsumerState<EnvironmentSetupScreen> {
  List<ToolVersionStatus> _tools = [];
  Map<String, String>? _gitInfo;
  bool _loading = false;

  @override
  void initState() {
    super.initState();
    Future.microtask(_runAudit);
  }

  Future<void> _runAudit() async {
    setState(() => _loading = true);
    final tools = await LocalExecutionEngine.auditLocalEnvironment();
    final scan = ref.read(activeWorkspaceScanProvider);
    Map<String, String>? gitInfo;
    if (scan != null) {
      gitInfo = await LocalExecutionEngine.inspectGitWorkspace(scan.rootPath);
    }
    if (!mounted) return;
    setState(() {
      _tools = tools;
      _gitInfo = gitInfo;
      _loading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return ListView(
      padding: const EdgeInsets.all(RmapTokens.space6),
      children: [
        Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Environment Setup & Git Workspace', style: theme.textTheme.headlineSmall),
                  Text('Live authorized local tool detection (Windows-first). Never installs system software without explicit approval.', style: theme.textTheme.bodySmall),
                ],
              ),
            ),
            FilledButton(onPressed: _loading ? null : _runAudit, child: Text(_loading ? 'Checking...' : 'Re-check Tools')),
          ],
        ),
        const SizedBox(height: RmapTokens.space4),
        if (_loading) const LinearProgressIndicator(),
        for (final t in _tools)
          Padding(
            padding: const EdgeInsets.only(bottom: 6),
            child: Card(
              child: ListTile(
                dense: true,
                leading: Icon(t.installed ? Icons.check_circle_outline : Icons.warning_amber_outlined,
                    size: 18, color: t.installed ? RmapTokens.success : RmapTokens.warning),
                title: Text(t.name, style: theme.textTheme.titleSmall),
                subtitle: Text('${t.versionOutput} · Official: ${t.officialSource}', style: theme.textTheme.bodySmall),
                trailing: Text(t.installed ? 'READY' : 'MISSING', style: theme.textTheme.labelSmall),
              ),
            ),
          ),
        if (_gitInfo != null) ...[
          const SizedBox(height: 12),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(RmapTokens.space4),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Git Workspace (${_gitInfo!['branch']})', style: theme.textTheme.titleSmall),
                  const SizedBox(height: 6),
                  Text(_gitInfo!['status'] ?? '', style: const TextStyle(fontFamily: 'Consolas', fontSize: 12)),
                ],
              ),
            ),
          ),
        ],
      ],
    );
  }
}

class PhasePlannerScreen extends ConsumerStatefulWidget {
  const PhasePlannerScreen({super.key});

  @override
  ConsumerState<PhasePlannerScreen> createState() => _PhasePlannerScreenState();
}

class _PhasePlannerScreenState extends ConsumerState<PhasePlannerScreen> {
  Map<String, dynamic>? _plan;
  String _mode = 'STANDARD';
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
    try {
      final res = await ref.read(apiClientProvider).get('/api/v1/projects/${project.id}/execution/phases');
      if (mounted) setState(() => _plan = res['data'] as Map<String, dynamic>?);
    } on ApiClientException catch (e) {
      if (mounted) setState(() => _error = e.message);
    }
  }

  Future<void> _generate() async {
    final project = ref.read(projectsControllerProvider).activeProject;
    if (project == null) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final res = await ref.read(apiClientProvider).post('/api/v1/projects/${project.id}/execution/phases/generate', {'mode': _mode});
      setState(() {
        _plan = res['data'] as Map<String, dynamic>?;
        _loading = false;
      });
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
    final phases = (_plan?['phases'] as List<dynamic>? ?? []);

    return ListView(
      padding: const EdgeInsets.all(RmapTokens.space6),
      children: [
        Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Dynamic Phase Planner', style: theme.textTheme.headlineSmall),
                  Text('Quick (1–4), Standard (5–10), or Custom phases with cycle detection and verified-work preservation.', style: theme.textTheme.bodySmall),
                ],
              ),
            ),
            SegmentedButton<String>(
              segments: const [
                ButtonSegment(value: 'QUICK', label: Text('Quick')),
                ButtonSegment(value: 'STANDARD', label: Text('Standard')),
                ButtonSegment(value: 'CUSTOM', label: Text('Custom')),
              ],
              selected: {_mode},
              onSelectionChanged: (s) => setState(() => _mode = s.first),
            ),
            const SizedBox(width: 8),
            FilledButton(
              onPressed: project == null || _loading ? null : _generate,
              child: const Text('Generate Plan'),
            ),
          ],
        ),
        const SizedBox(height: RmapTokens.space4),
        if (_error != null) Text(_error!, style: const TextStyle(color: RmapTokens.danger)),
        if (_loading) const LinearProgressIndicator(),
        for (final ph in phases)
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: Card(
              child: ListTile(
                title: Text('${ph['phaseId']} · ${ph['title']}', style: theme.textTheme.titleSmall),
                subtitle: Text('${ph['objective']}\nEffort: ${ph['estimatedEffort']} · Deps: ${(ph['dependencies'] as List<dynamic>? ?? []).join(", ")}'),
                trailing: Text(ph['status']?.toString() ?? 'PLANNED', style: theme.textTheme.labelSmall),
              ),
            ),
          ),
      ],
    );
  }
}

class MasterPromptsScreen extends ConsumerStatefulWidget {
  const MasterPromptsScreen({super.key});

  @override
  ConsumerState<MasterPromptsScreen> createState() => _MasterPromptsScreenState();
}

class _MasterPromptsScreenState extends ConsumerState<MasterPromptsScreen> {
  String _tool = 'ANTIGRAVITY';
  String _type = 'PHASE_MASTER';
  bool _enforceGate = true;
  List<dynamic> _history = [];
  Map<String, dynamic>? _active;
  String? _error;
  String? _info;

  @override
  void initState() {
    super.initState();
    Future.microtask(_load);
  }

  Future<void> _load() async {
    final project = ref.read(projectsControllerProvider).activeProject;
    if (project == null) return;
    try {
      final res = await ref.read(apiClientProvider).get('/api/v1/projects/${project.id}/execution/prompts');
      final list = res['data'] as List<dynamic>? ?? [];
      if (mounted) {
        setState(() {
          _history = list;
          _active = list.isNotEmpty ? list.first as Map<String, dynamic> : null;
        });
      }
    } on ApiClientException catch (e) {
      if (mounted) setState(() => _error = e.message);
    }
  }

  Future<void> _generate() async {
    final project = ref.read(projectsControllerProvider).activeProject;
    if (project == null) return;
    setState(() {
      _error = null;
      _info = null;
    });
    try {
      final res = await ref.read(apiClientProvider).post('/api/v1/projects/${project.id}/execution/prompts', {
        'phaseId': 'PH-001',
        'targetTool': _tool,
        'promptType': _type,
        'enforceMandatoryGate': _enforceGate,
      });
      setState(() {
        _active = res['data'] as Map<String, dynamic>?;
        _info = 'Generated 19-section Master Prompt v${_active?['versionNumber']} and logged to AI Usage Log (ACAD-14).';
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
        Text('Master Prompt Generation Engine', style: theme.textTheme.headlineSmall),
        Text('Generates 19-section tool-specific prompts, AGENTS.md / CLAUDE.md, and enforces ACAD-03 mandatory coverage.', style: theme.textTheme.bodySmall),
        const SizedBox(height: RmapTokens.space4),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            DropdownButton<String>(
              value: _tool,
              items: const ['ANTIGRAVITY', 'CLAUDE_CODE', 'OPENAI_CODEX', 'GITHUB_COPILOT', 'GENERIC']
                  .map((t) => DropdownMenuItem(value: t, child: Text(t)))
                  .toList(),
              onChanged: (v) => setState(() => _tool = v ?? 'ANTIGRAVITY'),
            ),
            DropdownButton<String>(
              value: _type,
              items: const ['PHASE_MASTER', 'TASK', 'PER_MEMBER', 'ERROR_FIX', 'SECURITY_REVIEW', 'TESTING', 'DEPLOYMENT']
                  .map((t) => DropdownMenuItem(value: t, child: Text(t)))
                  .toList(),
              onChanged: (v) => setState(() => _type = v ?? 'PHASE_MASTER'),
            ),
            FilterChip(
              label: const Text('Enforce Mandatory / ACAD-03 Gate'),
              selected: _enforceGate,
              onSelected: (v) => setState(() => _enforceGate = v),
            ),
            FilledButton(onPressed: _generate, child: const Text('Generate Prompt')),
            if (_active != null)
              OutlinedButton.icon(
                onPressed: () {
                  Clipboard.setData(ClipboardData(text: _active!['promptContent']?.toString() ?? ''));
                  setState(() => _info = 'Prompt copied to clipboard.');
                },
                icon: const Icon(Icons.copy, size: 15),
                label: const Text('Copy'),
              ),
          ],
        ),
        const SizedBox(height: 10),
        if (_error != null) Text(_error!, style: const TextStyle(color: RmapTokens.danger)),
        if (_info != null) Text(_info!, style: const TextStyle(color: RmapTokens.success)),
        if (_active != null) ...[
          const SizedBox(height: 10),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(RmapTokens.space4),
              child: SelectableText(
                _active!['promptContent']?.toString() ?? '',
                style: const TextStyle(fontFamily: 'Consolas', fontSize: 12),
              ),
            ),
          ),
        ],
        if (_history.length > 1) ...[
          const SizedBox(height: 12),
          Text('Prompt History (${_history.length} versions)', style: theme.textTheme.titleSmall),
        ],
      ],
    );
  }
}

class TasksAndKanbanScreen extends ConsumerStatefulWidget {
  const TasksAndKanbanScreen({super.key});

  @override
  ConsumerState<TasksAndKanbanScreen> createState() => _TasksAndKanbanScreenState();
}

class _TasksAndKanbanScreenState extends ConsumerState<TasksAndKanbanScreen> {
  List<dynamic> _tasks = [];
  final _titleCtrl = TextEditingController();
  String? _error;

  @override
  void initState() {
    super.initState();
    Future.microtask(_load);
  }

  @override
  void dispose() {
    _titleCtrl.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    final project = ref.read(projectsControllerProvider).activeProject;
    if (project == null) return;
    try {
      final res = await ref.read(apiClientProvider).get('/api/v1/projects/${project.id}/execution/tasks');
      if (mounted) setState(() => _tasks = res['data'] as List<dynamic>? ?? []);
    } on ApiClientException catch (e) {
      if (mounted) setState(() => _error = e.message);
    }
  }

  Future<void> _create() async {
    final project = ref.read(projectsControllerProvider).activeProject;
    if (project == null || _titleCtrl.text.trim().isEmpty) return;
    try {
      await ref.read(apiClientProvider).post('/api/v1/projects/${project.id}/execution/tasks', {
        'title': _titleCtrl.text.trim(),
        'phaseId': 'PH-001',
        'priority': 'HIGH',
      });
      _titleCtrl.clear();
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
        Text('Tasks, Ownership & Kanban', style: theme.textTheme.headlineSmall),
        Text('Tasks cannot be marked VERIFIED without real build/test/manual verification evidence.', style: theme.textTheme.bodySmall),
        const SizedBox(height: RmapTokens.space4),
        Row(
          children: [
            Expanded(child: TextField(controller: _titleCtrl, decoration: const InputDecoration(hintText: 'New engineering task title...'))),
            const SizedBox(width: 8),
            FilledButton(onPressed: _create, child: const Text('Add Task')),
          ],
        ),
        if (_error != null) Padding(padding: const EdgeInsets.only(top: 8), child: Text(_error!, style: const TextStyle(color: RmapTokens.danger))),
        const SizedBox(height: RmapTokens.space4),
        for (final t in _tasks)
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: Card(
              child: ListTile(
                title: Text('${t['taskCode']} · ${t['title']}', style: theme.textTheme.titleSmall),
                subtitle: Text('Phase: ${t['phaseId']} · Priority: ${t['priority']} · Owner: ${t['assignedMemberEmail'] ?? 'Unassigned'}'),
                trailing: Text(t['status']?.toString() ?? 'PLANNED', style: theme.textTheme.labelSmall),
              ),
            ),
          ),
      ],
    );
  }
}

class ErrorFixStudioScreen extends ConsumerStatefulWidget {
  const ErrorFixStudioScreen({super.key});

  @override
  ConsumerState<ErrorFixStudioScreen> createState() => _ErrorFixStudioScreenState();
}

class _ErrorFixStudioScreenState extends ConsumerState<ErrorFixStudioScreen> {
  final _logCtrl = TextEditingController();
  List<dynamic> _incidents = [];
  String? _error;

  @override
  void initState() {
    super.initState();
    Future.microtask(_load);
  }

  @override
  void dispose() {
    _logCtrl.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    final project = ref.read(projectsControllerProvider).activeProject;
    if (project == null) return;
    try {
      final res = await ref.read(apiClientProvider).get('/api/v1/projects/${project.id}/execution/errors');
      if (mounted) setState(() => _incidents = res['data'] as List<dynamic>? ?? []);
    } on ApiClientException catch (e) {
      if (mounted) setState(() => _error = e.message);
    }
  }

  Future<void> _diagnose() async {
    final project = ref.read(projectsControllerProvider).activeProject;
    if (project == null || _logCtrl.text.trim().isEmpty) return;
    try {
      await ref.read(apiClientProvider).post('/api/v1/projects/${project.id}/execution/errors/diagnose', {
        'title': 'Submitted Error Log',
        'sourceType': 'TERMINAL',
        'rawLog': _logCtrl.text.trim(),
      });
      _logCtrl.clear();
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
        Text('Error Fix Studio', style: theme.textTheme.headlineSmall),
        Text('Classifies build/test/runtime errors, generates targeted repair prompts, and enforces a bounded 3-attempt limit.', style: theme.textTheme.bodySmall),
        const SizedBox(height: RmapTokens.space4),
        TextField(controller: _logCtrl, maxLines: 4, decoration: const InputDecoration(hintText: 'Paste compiler output, stack trace, or test failure log...')),
        const SizedBox(height: 8),
        Align(alignment: Alignment.centerLeft, child: FilledButton(onPressed: _diagnose, child: const Text('Diagnose & Generate Repair Prompt'))),
        if (_error != null) Text(_error!, style: const TextStyle(color: RmapTokens.danger)),
        const SizedBox(height: RmapTokens.space4),
        for (final inc in _incidents)
          Padding(
            padding: const EdgeInsets.only(bottom: 10),
            child: Card(
              child: Padding(
                padding: const EdgeInsets.all(RmapTokens.space4),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('${inc['category']} · ${inc['status']} (Attempts: ${inc['repairAttempts']}/${inc['maxRepairAttempts']})', style: theme.textTheme.titleSmall),
                    const SizedBox(height: 4),
                    Text('Component: ${inc['affectedComponent']} · Root Cause: ${inc['likelyRootCause']}', style: theme.textTheme.bodySmall),
                    const SizedBox(height: 6),
                    SelectableText(inc['repairPrompt']?.toString() ?? '', style: const TextStyle(fontFamily: 'Consolas', fontSize: 11.5)),
                  ],
                ),
              ),
            ),
          ),
      ],
    );
  }
}

class TraceabilityAndVerificationScreen extends ConsumerStatefulWidget {
  const TraceabilityAndVerificationScreen({super.key});

  @override
  ConsumerState<TraceabilityAndVerificationScreen> createState() => _TraceabilityAndVerificationScreenState();
}

class _TraceabilityAndVerificationScreenState extends ConsumerState<TraceabilityAndVerificationScreen> {
  Map<String, dynamic>? _matrix;
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
      final res = await ref.read(apiClientProvider).get('/api/v1/projects/${project.id}/execution/traceability');
      if (mounted) setState(() => _matrix = res['data'] as Map<String, dynamic>?);
    } on ApiClientException catch (e) {
      if (mounted) setState(() => _error = e.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    final project = ref.watch(projectsControllerProvider).activeProject;
    final theme = Theme.of(context);
    if (project == null) return const Center(child: Text('Select a project first.'));

    final rows = (_matrix?['rows'] as List<dynamic>? ?? []);
    return ListView(
      padding: const EdgeInsets.all(RmapTokens.space6),
      children: [
        Text('Requirements Traceability & Evidence Verification', style: theme.textTheme.headlineSmall),
        Text('Strict separation of Planning Coverage, Implementation Coverage, and Verified Evidence Coverage.', style: theme.textTheme.bodySmall),
        const SizedBox(height: RmapTokens.space4),
        if (_error != null) Text(_error!, style: const TextStyle(color: RmapTokens.danger)),
        if (_matrix != null)
          Card(
            child: Padding(
              padding: const EdgeInsets.all(RmapTokens.space4),
              child: Wrap(
                spacing: 24,
                runSpacing: 8,
                children: [
                  Text('Total Reqs: ${_matrix!['totalRequirements']}'),
                  Text('Planned: ${_matrix!['planningCoveragePercent']}%'),
                  Text('Implemented: ${_matrix!['implementationCoveragePercent']}%'),
                  Text('Verified: ${_matrix!['verifiedCoveragePercent']}%', style: const TextStyle(fontWeight: FontWeight.w600)),
                ],
              ),
            ),
          ),
        const SizedBox(height: RmapTokens.space4),
        for (final r in rows)
          Card(
            child: ListTile(
              title: Text('${r['requirementId']} · ${r['title']}', style: theme.textTheme.titleSmall),
              subtitle: Text('Phases: ${(r['phases'] as List<dynamic>).join(", ")} · Tasks: ${(r['tasks'] as List<dynamic>).join(", ")} · Evidence: ${r['evidenceCount']}'),
              trailing: Text(r['state']?.toString() ?? '', style: theme.textTheme.labelSmall),
            ),
          ),
      ],
    );
  }
}
