import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/theme/rmap_tokens.dart';
import '../shell/rmap_shell.dart';
import 'project_state.dart';

class MyProjectsScreen extends ConsumerStatefulWidget {
  const MyProjectsScreen({super.key});

  @override
  ConsumerState<MyProjectsScreen> createState() => _MyProjectsScreenState();
}

class _MyProjectsScreenState extends ConsumerState<MyProjectsScreen> {
  final _searchCtrl = TextEditingController();
  String _statusFilter = 'ACTIVE';

  @override
  void initState() {
    super.initState();
    Future.microtask(() => ref.read(projectsControllerProvider.notifier).refresh(status: _statusFilter));
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(projectsControllerProvider);
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
                  Text('My Projects', style: theme.textTheme.headlineSmall),
                  const SizedBox(height: 4),
                  Text('Universal software engineering projects persisted in MongoDB.', style: theme.textTheme.bodySmall),
                ],
              ),
            ),
            FilledButton.icon(
              onPressed: () => context.go('/projects/create'),
              icon: const Icon(Icons.add, size: 16),
              label: const Text('New Project'),
            ),
          ],
        ),
        const SizedBox(height: RmapTokens.space4),
        Row(
          children: [
            Expanded(
              child: TextField(
                controller: _searchCtrl,
                decoration: const InputDecoration(prefixIcon: Icon(Icons.search, size: 16), hintText: 'Search by name, description, or archetype...'),
                onSubmitted: (q) => ref.read(projectsControllerProvider.notifier).refresh(query: q, status: _statusFilter),
              ),
            ),
            const SizedBox(width: RmapTokens.space2),
            SegmentedButton<String>(
              segments: const [
                ButtonSegment(value: 'ACTIVE', label: Text('Active')),
                ButtonSegment(value: 'ARCHIVED', label: Text('Archived')),
              ],
              selected: {_statusFilter},
              onSelectionChanged: (s) {
                setState(() => _statusFilter = s.first);
                ref.read(projectsControllerProvider.notifier).refresh(query: _searchCtrl.text, status: _statusFilter);
              },
            ),
          ],
        ),
        const SizedBox(height: RmapTokens.space4),
        if (state.error != null)
          Card(child: Padding(padding: const EdgeInsets.all(12), child: Text(state.error!, style: const TextStyle(color: RmapTokens.danger)))),
        if (state.loading)
          const Padding(padding: EdgeInsets.all(24), child: Center(child: CircularProgressIndicator(strokeWidth: 2)))
        else if (state.projects.isEmpty)
          Card(
            child: Padding(
              padding: const EdgeInsets.all(RmapTokens.space6),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('No projects found', style: theme.textTheme.titleMedium),
                  const SizedBox(height: 6),
                  Text('Create your first project using the adaptive wizard to unlock discovery, specs, architecture, and AI agents.', style: theme.textTheme.bodySmall),
                ],
              ),
            ),
          )
        else
          for (final p in state.projects)
            Padding(
              padding: const EdgeInsets.only(bottom: RmapTokens.space3),
              child: Card(
                child: ListTile(
                  title: Text(p.name, style: theme.textTheme.titleSmall),
                  subtitle: Text('${p.mode} · ${p.archetypes.isEmpty ? "Custom" : p.archetypes.join(", ")} · Spec v${p.activeSpecVersion}'),
                  trailing: Wrap(
                    spacing: 6,
                    children: [
                      OutlinedButton(
                        onPressed: () {
                          ref.read(projectsControllerProvider.notifier).selectProject(p);
                          ref.read(activeProjectModeProvider.notifier).state = p.toContextMode();
                          context.go('/discovery/requirements');
                        },
                        child: Text(state.activeProject?.id == p.id ? 'Active' : 'Open'),
                      ),
                      IconButton(
                        tooltip: 'Duplicate',
                        icon: const Icon(Icons.copy_outlined, size: 16),
                        onPressed: () => ref.read(projectsControllerProvider.notifier).duplicate(p.id),
                      ),
                      IconButton(
                        tooltip: p.status == 'ARCHIVED' ? 'Restore' : 'Archive',
                        icon: Icon(p.status == 'ARCHIVED' ? Icons.unarchive_outlined : Icons.archive_outlined, size: 16),
                        onPressed: () => ref.read(projectsControllerProvider.notifier).toggleArchive(p),
                      ),
                    ],
                  ),
                ),
              ),
            ),
      ],
    );
  }
}

class CreateProjectWizardScreen extends ConsumerStatefulWidget {
  const CreateProjectWizardScreen({super.key});

  @override
  ConsumerState<CreateProjectWizardScreen> createState() => _CreateProjectWizardScreenState();
}

class _CreateProjectWizardScreenState extends ConsumerState<CreateProjectWizardScreen> {
  final _nameCtrl = TextEditingController();
  final _descCtrl = TextEditingController();
  final _problemCtrl = TextEditingController();
  final _customArchetypeCtrl = TextEditingController();
  final _constraintsCtrl = TextEditingController();
  final _techPrefsCtrl = TextEditingController();
  final _deadlineCtrl = TextEditingController();
  final _budgetCtrl = TextEditingController();

  String _mode = 'INDIVIDUAL';
  int _teamSize = 1;
  final Set<String> _selectedArchetypes = {'Full-Stack'};
  final Set<String> _inputSources = {'Project Idea'};

  static const _modes = ['INDIVIDUAL', 'CLIENT', 'TEAM', 'ACADEMIC', 'STARTUP', 'TESTING', 'MAINTENANCE'];
  static const _archetypeOptions = [
    'Web Application', 'Mobile Application', 'Desktop Application', 'Full-Stack',
    'E-Commerce', 'SaaS', 'APIs & Microservices', 'Machine Learning', 'Data Science',
    'Agentic AI', 'IoT', 'Automation', 'Cybersecurity', 'Hackathon',
  ];
  static const _inputOptions = [
    'Project Idea', 'Manual Requirements', 'Client Documents', 'Assignment Brief',
    'Existing Repository', 'Source ZIP', 'ML Dataset', 'Design References',
  ];

  @override
  void dispose() {
    _nameCtrl.dispose();
    _descCtrl.dispose();
    _problemCtrl.dispose();
    _customArchetypeCtrl.dispose();
    _constraintsCtrl.dispose();
    _techPrefsCtrl.dispose();
    _deadlineCtrl.dispose();
    _budgetCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_nameCtrl.text.trim().length < 2) return;
    final archetypes = [..._selectedArchetypes];
    if (_customArchetypeCtrl.text.trim().isNotEmpty) {
      archetypes.add(_customArchetypeCtrl.text.trim());
    }
    final created = await ref.read(projectsControllerProvider.notifier).createProject({
      'name': _nameCtrl.text.trim(),
      'description': _descCtrl.text.trim(),
      'problemStatement': _problemCtrl.text.trim(),
      'mode': _mode,
      'archetypes': archetypes,
      'teamSize': _teamSize,
      'deadline': _deadlineCtrl.text.trim(),
      'budget': _budgetCtrl.text.trim(),
      'technologyPreferences': _splitComma(_techPrefsCtrl.text),
      'mandatoryConstraints': _splitComma(_constraintsCtrl.text),
      'inputSources': _inputSources.toList(),
    });
    if (created != null && mounted) {
      ref.read(activeProjectModeProvider.notifier).state = created.toContextMode();
      context.go('/discovery/requirements');
    }
  }

  List<String> _splitComma(String s) => s.split(',').map((e) => e.trim()).where((e) => e.isNotEmpty).toList();

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(projectsControllerProvider);
    final theme = Theme.of(context);
    final showTeam = _mode == 'TEAM' || _mode == 'ACADEMIC' || _mode == 'CLIENT' || _mode == 'STARTUP';
    final showBudget = _mode == 'CLIENT' || _mode == 'STARTUP';

    return ListView(
      padding: const EdgeInsets.all(RmapTokens.space6),
      children: [
        Text('Create Project Wizard', style: theme.textTheme.headlineSmall),
        const SizedBox(height: 4),
        Text('Adapts dynamically to your project mode, combined archetypes, and input sources.', style: theme.textTheme.bodySmall),
        const SizedBox(height: RmapTokens.space4),
        if (state.error != null)
          Card(child: Padding(padding: const EdgeInsets.all(12), child: Text(state.error!, style: const TextStyle(color: RmapTokens.danger)))),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(RmapTokens.space5),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('1. Project Mode & Input Methods', style: theme.textTheme.titleSmall),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 6,
                  runSpacing: 6,
                  children: _modes
                      .map((m) => ChoiceChip(
                            label: Text(m),
                            selected: _mode == m,
                            onSelected: (_) => setState(() => _mode = m),
                          ))
                      .toList(),
                ),
                const SizedBox(height: 12),
                Text('Input Sources (combine multiple)', style: theme.textTheme.labelSmall),
                const SizedBox(height: 6),
                Wrap(
                  spacing: 6,
                  runSpacing: 6,
                  children: _inputOptions
                      .map((src) => FilterChip(
                            label: Text(src),
                            selected: _inputSources.contains(src),
                            onSelected: (sel) => setState(() => sel ? _inputSources.add(src) : _inputSources.remove(src)),
                          ))
                      .toList(),
                ),
                const Divider(height: 24),
                Text('2. Core Details & Combined Archetypes', style: theme.textTheme.titleSmall),
                const SizedBox(height: 8),
                TextField(controller: _nameCtrl, decoration: const InputDecoration(labelText: 'Project Name *')),
                const SizedBox(height: 10),
                TextField(controller: _descCtrl, maxLines: 2, decoration: const InputDecoration(labelText: 'Project Description')),
                const SizedBox(height: 10),
                TextField(controller: _problemCtrl, maxLines: 2, decoration: const InputDecoration(labelText: 'Problem Statement')),
                const SizedBox(height: 12),
                Wrap(
                  spacing: 6,
                  runSpacing: 6,
                  children: _archetypeOptions
                      .map((a) => FilterChip(
                            label: Text(a),
                            selected: _selectedArchetypes.contains(a),
                            onSelected: (sel) => setState(() => sel ? _selectedArchetypes.add(a) : _selectedArchetypes.remove(a)),
                          ))
                      .toList(),
                ),
                const SizedBox(height: 10),
                TextField(controller: _customArchetypeCtrl, decoration: const InputDecoration(labelText: 'Custom Archetype (optional)')),
                const Divider(height: 24),
                Text('3. Constraints, Timeline & Preferences', style: theme.textTheme.titleSmall),
                const SizedBox(height: 8),
                TextField(
                  controller: _constraintsCtrl,
                  decoration: const InputDecoration(labelText: 'Mandatory Constraints (comma-separated, e.g., Must use PostgreSQL)'),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: _techPrefsCtrl,
                  decoration: const InputDecoration(labelText: 'Technology Preferences (optional, comma-separated)'),
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(child: TextField(controller: _deadlineCtrl, decoration: const InputDecoration(labelText: 'Deadline (optional)'))),
                    if (showBudget) ...[
                      const SizedBox(width: 10),
                      Expanded(child: TextField(controller: _budgetCtrl, decoration: const InputDecoration(labelText: 'Budget'))),
                    ],
                    if (showTeam) ...[
                      const SizedBox(width: 10),
                      SizedBox(
                        width: 130,
                        child: DropdownButtonFormField<int>(
                          initialValue: _teamSize,
                          decoration: const InputDecoration(labelText: 'Team Size'),
                          items: [1, 2, 3, 4, 5, 8, 12].map((n) => DropdownMenuItem(value: n, child: Text('$n'))).toList(),
                          onChanged: (v) => setState(() => _teamSize = v ?? 1),
                        ),
                      ),
                    ],
                  ],
                ),
                const SizedBox(height: 18),
                FilledButton(
                  onPressed: state.loading ? null : _submit,
                  child: Text(state.loading ? 'Creating...' : 'Create Project & Open Specification'),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}
