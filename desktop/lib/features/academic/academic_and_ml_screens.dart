import 'package:file_selector/file_selector.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path/path.dart' as p;
import '../../core/network/api_client.dart';
import '../../core/theme/rmap_tokens.dart';
import '../projects/project_state.dart';

class AcademicStudioScreen extends ConsumerStatefulWidget {
  const AcademicStudioScreen({super.key});

  @override
  ConsumerState<AcademicStudioScreen> createState() => _AcademicStudioScreenState();
}

class _AcademicStudioScreenState extends ConsumerState<AcademicStudioScreen> {
  Map<String, dynamic>? _assessment;
  Map<String, dynamic>? _coverage;
  String? _error;
  final _rubTitleCtrl = TextEditingController();
  final _rubMarksCtrl = TextEditingController(text: '25');

  @override
  void initState() {
    super.initState();
    Future.microtask(_load);
  }

  @override
  void dispose() {
    _rubTitleCtrl.dispose();
    _rubMarksCtrl.dispose();
    super.dispose();
  }

  Future<void> _addRubricCriterion() async {
    final project = ref.read(projectsControllerProvider).activeProject;
    if (project == null || _assessment == null || _rubTitleCtrl.text.trim().isEmpty) return;
    final list = List<Map<String, dynamic>>.from(
      (_assessment!['rubricCriteria'] as List<dynamic>? ?? []).map((e) => Map<String, dynamic>.from(e as Map)),
    );
    list.add({
      'id': 'RUB-${(list.length + 1).toString().padLeft(3, '0')}',
      'title': _rubTitleCtrl.text.trim(),
      'maxMarks': double.tryParse(_rubMarksCtrl.text.trim()) ?? 0.0,
      'performanceBandSummary': 'Target Band',
      'sourceRef': 'Manual Entry',
      'linkedRequirementIds': <String>[],
      'plannedCoverageStatus': 'UNMAPPED',
      'verifiedEvidenceStatus': 'NOT_VERIFIED',
    });
    final payload = Map<String, dynamic>.from(_assessment!)..['rubricCriteria'] = list;
    try {
      await ref.read(apiClientProvider).put('/api/v1/projects/${project.id}/academic', payload);
      _rubTitleCtrl.clear();
      await _load();
    } on ApiClientException catch (e) {
      setState(() => _error = e.message);
    }
  }

  Future<void> _load() async {
    final project = ref.read(projectsControllerProvider).activeProject;
    if (project == null) return;
    try {
      final a = await ref.read(apiClientProvider).get('/api/v1/projects/${project.id}/academic');
      final c = await ref.read(apiClientProvider).get('/api/v1/projects/${project.id}/academic/rubric-coverage');
      setState(() {
        _assessment = a['data'] as Map<String, dynamic>?;
        _coverage = c['data'] as Map<String, dynamic>?;
      });
    } on ApiClientException catch (e) {
      setState(() => _error = e.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    final project = ref.watch(projectsControllerProvider).activeProject;
    final theme = Theme.of(context);
    if (project == null) return const Center(child: Text('Select an Academic project first.'));

    final inconsistencies = (_assessment?['detectedInconsistencies'] as List<dynamic>? ?? []);
    final warnings = (_assessment?['integrityWarnings'] as List<dynamic>? ?? []);

    return ListView(
      padding: const EdgeInsets.all(RmapTokens.space6),
      children: [
        Text('Academic Intelligence Studio (ACAD-01 – ACAD-16)', style: theme.textTheme.headlineSmall),
        const SizedBox(height: 4),
        Text(
          'Planned rubric coverage is strictly separated from verified implementation evidence. Rmap never guarantees academic marks.',
          style: theme.textTheme.bodySmall,
        ),
        const SizedBox(height: RmapTokens.space4),
        if (_error != null) Text(_error!, style: const TextStyle(color: RmapTokens.danger)),
        if (_coverage != null)
          Card(
            child: Padding(
              padding: const EdgeInsets.all(RmapTokens.space4),
              child: Wrap(
                spacing: 20,
                runSpacing: 8,
                children: [
                  Text('Declared Marks: ${_coverage!['declaredTotalMarks']}'),
                  Text('Rubric Sum: ${_coverage!['rubricCriteriaSum']}'),
                  Text('Planned Mapped: ${_coverage!['plannedMappedCriteria']} / ${_coverage!['totalCriteria']}'),
                  Text('Verified Evidence: ${_coverage!['verifiedEvidenceCriteria']}'),
                ],
              ),
            ),
          ),
        if (inconsistencies.isNotEmpty) ...[
          const SizedBox(height: 12),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(RmapTokens.space4),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Detected Inconsistencies (Review Required)', style: TextStyle(color: RmapTokens.danger, fontWeight: FontWeight.w600)),
                  for (final i in inconsistencies) Text('• $i', style: const TextStyle(color: RmapTokens.danger, fontSize: 12.5)),
                ],
              ),
            ),
          ),
        ],
        const SizedBox(height: 12),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(RmapTokens.space4),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Add Rubric Criterion (live mark-sum audit)', style: theme.textTheme.titleSmall),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Expanded(child: TextField(controller: _rubTitleCtrl, decoration: const InputDecoration(hintText: 'Criterion title'))),
                    const SizedBox(width: 8),
                    SizedBox(width: 90, child: TextField(controller: _rubMarksCtrl, decoration: const InputDecoration(hintText: 'Marks'))),
                    const SizedBox(width: 8),
                    OutlinedButton(onPressed: _addRubricCriterion, child: const Text('Add')),
                  ],
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(RmapTokens.space4),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Academic Integrity Safeguards', style: theme.textTheme.titleSmall),
                const SizedBox(height: 6),
                for (final w in warnings) Text('• $w', style: theme.textTheme.bodySmall),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class DatasetWorkspaceScreen extends ConsumerStatefulWidget {
  const DatasetWorkspaceScreen({super.key});

  @override
  ConsumerState<DatasetWorkspaceScreen> createState() => _DatasetWorkspaceScreenState();
}

class _DatasetWorkspaceScreenState extends ConsumerState<DatasetWorkspaceScreen> {
  List<dynamic> _profiles = [];
  List<dynamic> _experiments = [];
  bool _loading = false;
  String? _error;
  final _targetCtrl = TextEditingController();
  final _expNameCtrl = TextEditingController();
  final _modelFamilyCtrl = TextEditingController(text: 'LogisticRegression');
  String _taskType = 'CLASSIFICATION';

  @override
  void initState() {
    super.initState();
    Future.microtask(_load);
  }

  @override
  void dispose() {
    _targetCtrl.dispose();
    _expNameCtrl.dispose();
    _modelFamilyCtrl.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    final project = ref.read(projectsControllerProvider).activeProject;
    if (project == null) return;
    try {
      final res = await ref.read(apiClientProvider).get('/api/v1/projects/${project.id}/datasets');
      final expRes = await ref.read(apiClientProvider).get('/api/v1/projects/${project.id}/delivery/ml/experiments');
      if (mounted) {
        setState(() {
          _profiles = res['data'] as List<dynamic>? ?? [];
          _experiments = expRes['data'] as List<dynamic>? ?? [];
        });
      }
    } on ApiClientException catch (e) {
      if (mounted) setState(() => _error = e.message);
    }
  }

  Future<void> _createExperiment() async {
    final project = ref.read(projectsControllerProvider).activeProject;
    if (project == null || _expNameCtrl.text.trim().isEmpty) return;
    try {
      await ref.read(apiClientProvider).post('/api/v1/projects/${project.id}/delivery/ml/experiments', {
        'name': _expNameCtrl.text.trim(),
        'taskType': _taskType,
        'modelFamily': _modelFamilyCtrl.text.trim(),
        'datasetFingerprint': _profiles.isNotEmpty ? _profiles.first['sha256Fingerprint'] : 'unlinked',
      });
      _expNameCtrl.clear();
      await _load();
    } on ApiClientException catch (e) {
      setState(() => _error = e.message);
    }
  }

  Future<void> _uploadCsv() async {
    final project = ref.read(projectsControllerProvider).activeProject;
    if (project == null) return;
    final file = await openFile(acceptedTypeGroups: [const XTypeGroup(label: 'CSV', extensions: ['csv'])]);
    if (file == null) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      await ref.read(apiClientProvider).postMultipart(
            '/api/v1/projects/${project.id}/datasets',
            filePath: file.path,
            fileName: p.basename(file.path),
            fields: {
              'targetColumn': _targetCtrl.text.trim(),
              'mlTaskType': _taskType,
            },
          );
      await _load();
      setState(() => _loading = false);
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
    if (project == null) return const Center(child: Text('Select an ML project first.'));

    return ListView(
      padding: const EdgeInsets.all(RmapTokens.space6),
      children: [
        Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Dataset & ML Intelligence Workspace', style: theme.textTheme.headlineSmall),
                  Text('Deterministic CSV profiling, SHA-256 fingerprints, leakage warnings, and experiment planning (never fabricated metrics).', style: theme.textTheme.bodySmall),
                ],
              ),
            ),
            FilledButton.icon(
              onPressed: _loading ? null : _uploadCsv,
              icon: const Icon(Icons.table_chart_outlined, size: 16),
              label: const Text('Profile CSV Dataset'),
            ),
          ],
        ),
        const SizedBox(height: RmapTokens.space3),
        Row(
          children: [
            Expanded(child: TextField(controller: _targetCtrl, decoration: const InputDecoration(labelText: 'Target Column (optional)'))),
            const SizedBox(width: 10),
            SizedBox(
              width: 200,
              child: DropdownButtonFormField<String>(
                initialValue: _taskType,
                decoration: const InputDecoration(labelText: 'ML Task'),
                items: const ['CLASSIFICATION', 'REGRESSION', 'CLUSTERING', 'NLP', 'TIME_SERIES']
                    .map((t) => DropdownMenuItem(value: t, child: Text(t)))
                    .toList(),
                onChanged: (v) => setState(() => _taskType = v ?? 'CLASSIFICATION'),
              ),
            ),
          ],
        ),
        const SizedBox(height: RmapTokens.space4),
        if (_error != null) Text(_error!, style: const TextStyle(color: RmapTokens.danger)),
        if (_loading) const LinearProgressIndicator(),
        for (final ds in _profiles)
          Padding(
            padding: const EdgeInsets.only(bottom: 10),
            child: Card(
              child: Padding(
                padding: const EdgeInsets.all(RmapTokens.space4),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('${ds['fileName']} · ${ds['rowCount']} rows × ${ds['columnCount']} cols (${ds['mlTaskType']})', style: theme.textTheme.titleSmall),
                    Text('Duplicates: ${ds['duplicateRowCount']} · SHA-256: ${(ds['sha256Fingerprint']?.toString() ?? '').substring(0, 16)}...', style: theme.textTheme.bodySmall),
                    for (final w in (ds['leakageRiskWarnings'] as List<dynamic>? ?? []))
                      Text('Leakage warning: $w', style: const TextStyle(color: RmapTokens.warning, fontSize: 12)),
                  ],
                ),
              ),
            ),
          ),
        const Divider(height: 24),
        Text('ML Experiment Studio (${_experiments.length} tracked — real imported metrics only)', style: theme.textTheme.titleSmall),
        const SizedBox(height: 8),
        Row(
          children: [
            Expanded(child: TextField(controller: _expNameCtrl, decoration: const InputDecoration(hintText: 'Experiment name (e.g., Baseline LogReg)'))),
            const SizedBox(width: 8),
            SizedBox(width: 180, child: TextField(controller: _modelFamilyCtrl, decoration: const InputDecoration(hintText: 'Model family'))),
            const SizedBox(width: 8),
            OutlinedButton(onPressed: _createExperiment, child: const Text('Track Experiment')),
          ],
        ),
        const SizedBox(height: 8),
        for (final exp in _experiments)
          Card(
            child: ListTile(
              title: Text('${exp['name']} · ${exp['modelFamily']} (${exp['status']})', style: theme.textTheme.titleSmall),
              subtitle: Text('Source: ${exp['resultSource'] ?? 'Pending real run import'} · Metrics: ${exp['importedMetrics']}'),
            ),
          ),
      ],
    );
  }
}
