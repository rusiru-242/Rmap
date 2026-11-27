import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/network/api_client.dart';
import '../../core/theme/rmap_tokens.dart';
import '../projects/project_state.dart';

class AgentManagerScreen extends ConsumerStatefulWidget {
  const AgentManagerScreen({super.key});

  @override
  ConsumerState<AgentManagerScreen> createState() => _AgentManagerScreenState();
}

class _AgentManagerScreenState extends ConsumerState<AgentManagerScreen> {
  Map<String, dynamic>? _aiStatus;
  List<dynamic> _workflows = [];
  bool _loading = false;
  String? _error;
  bool _checkpoints = true;
  final Set<String> _selectedAgents = {'DISCOVERY', 'ARCHITECT', 'ENGINEERING', 'QUALITY_DELIVERY'};

  @override
  void initState() {
    super.initState();
    Future.microtask(_load);
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final statusRes = await ref.read(apiClientProvider).get('/api/v1/ai/status');
      List<dynamic> wfs = [];
      final project = ref.read(projectsControllerProvider).activeProject;
      if (project != null) {
        final wfRes = await ref.read(apiClientProvider).get('/api/v1/projects/${project.id}/ai/workflows');
        wfs = wfRes['data'] as List<dynamic>? ?? [];
      }
      setState(() {
        _aiStatus = statusRes['data'] as Map<String, dynamic>?;
        _workflows = wfs;
        _loading = false;
      });
    } on ApiClientException catch (e) {
      setState(() {
        _error = e.message;
        _loading = false;
      });
    }
  }

  Future<void> _runWorkflow() async {
    final project = ref.read(projectsControllerProvider).activeProject;
    if (project == null) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      await ref.read(apiClientProvider).post('/api/v1/projects/${project.id}/ai/workflows', {
        'agents': _selectedAgents.toList(),
        'requireHumanCheckpoints': _checkpoints,
        'additionalInstruction': '',
      });
      await _load();
    } on ApiClientException catch (e) {
      setState(() {
        _error = e.message;
        _loading = false;
      });
    }
  }

  Future<void> _workflowAction(String wfId, String action) async {
    final project = ref.read(projectsControllerProvider).activeProject;
    if (project == null) return;
    try {
      await ref.read(apiClientProvider).post('/api/v1/projects/${project.id}/ai/workflows/$wfId/$action');
      await _load();
    } on ApiClientException catch (e) {
      setState(() => _error = e.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    final project = ref.watch(projectsControllerProvider).activeProject;
    final theme = Theme.of(context);
    final configured = _aiStatus?['configured'] == true;

    return ListView(
      padding: const EdgeInsets.all(RmapTokens.space6),
      children: [
        Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('AI Agent Manager & Workflow Orchestrator', style: theme.textTheme.headlineSmall),
                  Text(
                    'Provider: ${_aiStatus?['provider'] ?? 'google-gemini'} · Model: ${_aiStatus?['model'] ?? 'gemini-2.5-flash'}',
                    style: theme.textTheme.bodySmall,
                  ),
                ],
              ),
            ),
            if (project != null)
              FilledButton(
                onPressed: _loading ? null : _runWorkflow,
                child: const Text('Run Agent Workflow'),
              ),
          ],
        ),
        const SizedBox(height: RmapTokens.space4),
        if (!configured)
          Card(
            child: Padding(
              padding: const EdgeInsets.all(RmapTokens.space4),
              child: Text(
                'Configuration Required: GEMINI_API_KEY is not set on the backend. '
                'Live Gemini execution is truthfully disabled (no fake AI responses).',
                style: const TextStyle(color: RmapTokens.warning, fontSize: 12.5),
              ),
            ),
          ),
        if (_error != null)
          Padding(
            padding: const EdgeInsets.only(top: 8),
            child: Text(_error!, style: const TextStyle(color: RmapTokens.danger)),
          ),
        const SizedBox(height: RmapTokens.space4),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(RmapTokens.space4),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Conditional Agent Pipeline', style: theme.textTheme.titleSmall),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 6,
                  runSpacing: 6,
                  children: ['DISCOVERY', 'ARCHITECT', 'ENGINEERING', 'QUALITY_DELIVERY']
                      .map((a) => FilterChip(
                            label: Text(a),
                            selected: _selectedAgents.contains(a),
                            onSelected: (sel) => setState(() => sel ? _selectedAgents.add(a) : _selectedAgents.remove(a)),
                          ))
                      .toList(),
                ),
                CheckboxListTile(
                  dense: true,
                  contentPadding: EdgeInsets.zero,
                  value: _checkpoints,
                  onChanged: (v) => setState(() => _checkpoints = v ?? true),
                  title: const Text('Require human approval checkpoints after Discovery and Architecture', style: TextStyle(fontSize: 12.5)),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: RmapTokens.space4),
        if (_loading) const LinearProgressIndicator(),
        for (final wf in _workflows)
          Padding(
            padding: const EdgeInsets.only(bottom: 10),
            child: Card(
              child: Padding(
                padding: const EdgeInsets.all(RmapTokens.space4),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text('Workflow ${wf['id']} · ${wf['status']}', style: theme.textTheme.titleSmall),
                        ),
                        if (wf['status'] == 'AWAITING_APPROVAL')
                          FilledButton(onPressed: () => _workflowAction(wf['id'].toString(), 'approve'), child: const Text('Approve')),
                        if (wf['status'] == 'FAILED')
                          OutlinedButton(onPressed: () => _workflowAction(wf['id'].toString(), 'retry'), child: const Text('Retry')),
                      ],
                    ),
                    const SizedBox(height: 6),
                    Text(
                      'Tokens: ${(wf['totalPromptTokens'] ?? 0) + (wf['totalCompletionTokens'] ?? 0)} · '
                      'Est. Cost: \$${((wf['totalEstimatedCostUsd'] as num?) ?? 0).toStringAsFixed(5)}',
                      style: theme.textTheme.bodySmall,
                    ),
                    if (wf['errorMessage'] != null)
                      Text(wf['errorMessage'].toString(), style: const TextStyle(color: RmapTokens.danger, fontSize: 12)),
                  ],
                ),
              ),
            ),
          ),
      ],
    );
  }
}
