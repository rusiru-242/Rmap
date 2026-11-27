import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/network/api_client.dart';
import '../../core/theme/rmap_tokens.dart';
import '../projects/project_state.dart';

class ProjectChatPanel extends ConsumerStatefulWidget {
  const ProjectChatPanel({super.key});

  @override
  ConsumerState<ProjectChatPanel> createState() => _ProjectChatPanelState();
}

class _ProjectChatPanelState extends ConsumerState<ProjectChatPanel> {
  final _msgCtrl = TextEditingController();
  String? _loadedProjectId;
  List<Map<String, dynamic>> _conversations = [];
  String? _conversationId;
  List<dynamic> _messages = [];
  bool _sending = false;
  String? _error;

  @override
  void dispose() {
    _msgCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadForProject(String projectId) async {
    setState(() {
      _loadedProjectId = projectId;
      _conversations = [];
      _conversationId = null;
      _messages = [];
      _error = null;
    });
    try {
      final res = await ref.read(apiClientProvider).get('/api/v1/projects/$projectId/chat/conversations');
      if (!mounted || _loadedProjectId != projectId) return;
      final list = (res['data'] as List<dynamic>? ?? [])
          .map((e) => Map<String, dynamic>.from(e as Map))
          .toList();
      setState(() {
        _conversations = list;
        if (list.isNotEmpty) {
          _conversationId = list.first['id']?.toString();
          _messages = (list.first['messages'] as List<dynamic>? ?? []);
        }
      });
    } on ApiClientException catch (e) {
      if (mounted && _loadedProjectId == projectId) {
        setState(() => _error = e.message);
      }
    }
  }

  Future<void> _newSession(String projectId, String projectName) async {
    try {
      final res = await ref.read(apiClientProvider).post(
        '/api/v1/projects/$projectId/chat/conversations',
        {'title': '$projectName Session ${_conversations.length + 1}'},
      );
      final created = Map<String, dynamic>.from(res['data'] as Map);
      setState(() {
        _conversations = [created, ..._conversations];
        _conversationId = created['id']?.toString();
        _messages = [];
        _error = null;
      });
    } on ApiClientException catch (e) {
      setState(() => _error = e.message);
    }
  }

  Future<void> _deleteCurrentSession(String projectId) async {
    final cid = _conversationId;
    if (cid == null) return;
    try {
      await ref.read(apiClientProvider).delete('/api/v1/projects/$projectId/chat/conversations/$cid');
      await _loadForProject(projectId);
    } on ApiClientException catch (e) {
      setState(() => _error = e.message);
    }
  }

  Future<void> _send() async {
    final project = ref.read(projectsControllerProvider).activeProject;
    final text = _msgCtrl.text.trim();
    if (project == null || text.isEmpty) return;

    setState(() {
      _sending = true;
      _error = null;
    });
    try {
      final api = ref.read(apiClientProvider);
      if (_conversationId == null) {
        final created = await api.post('/api/v1/projects/${project.id}/chat/conversations', {'title': project.name});
        final conv = Map<String, dynamic>.from(created['data'] as Map);
        _conversationId = conv['id']?.toString();
        _conversations = [conv, ..._conversations];
      }
      final res = await api.post(
        '/api/v1/projects/${project.id}/chat/conversations/$_conversationId/messages',
        {'message': text},
      );
      _msgCtrl.clear();
      final updated = Map<String, dynamic>.from(res['data'] as Map);
      setState(() {
        _messages = (updated['messages'] as List<dynamic>? ?? []);
        _sending = false;
      });
    } on ApiClientException catch (e) {
      setState(() {
        _error = e.message;
        _sending = false;
      });
    }
  }

  Future<void> _approveProposal(String proposalId) async {
    final project = ref.read(projectsControllerProvider).activeProject;
    if (project == null || _conversationId == null) return;
    try {
      await ref.read(apiClientProvider).post(
            '/api/v1/projects/${project.id}/chat/conversations/$_conversationId/proposals/$proposalId/approve',
          );
      await _loadForProject(project.id);
    } on ApiClientException catch (e) {
      setState(() => _error = e.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    final project = ref.watch(projectsControllerProvider).activeProject;
    final theme = Theme.of(context);

    // Strictly isolate chat state per active project to prevent cross-project UI bleed.
    if (project != null && project.id != _loadedProjectId) {
      Future.microtask(() => _loadForProject(project.id));
    } else if (project == null && _loadedProjectId != null) {
      _loadedProjectId = null;
      _conversations = [];
      _conversationId = null;
      _messages = [];
    }

    return Container(
      color: theme.colorScheme.surface,
      padding: const EdgeInsets.all(RmapTokens.space4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.auto_awesome_outlined, size: 16, color: theme.colorScheme.primary),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  'Project-Aware AI Assistant',
                  style: theme.textTheme.titleSmall,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              if (project != null) ...[
                IconButton(
                  tooltip: 'New Chat Session',
                  icon: const Icon(Icons.add_comment_outlined, size: 15),
                  visualDensity: VisualDensity.compact,
                  onPressed: () => _newSession(project.id, project.name),
                ),
                if (_conversationId != null)
                  IconButton(
                    tooltip: 'Delete Session',
                    icon: const Icon(Icons.delete_outline, size: 15),
                    visualDensity: VisualDensity.compact,
                    onPressed: () => _deleteCurrentSession(project.id),
                  ),
              ],
            ],
          ),
          if (project != null && _conversations.isNotEmpty) ...[
            const SizedBox(height: 6),
            DropdownButtonHideUnderline(
              child: DropdownButton<String>(
                isExpanded: true,
                isDense: true,
                value: _conversationId,
                style: theme.textTheme.bodySmall,
                items: _conversations
                    .map((c) => DropdownMenuItem<String>(
                          value: c['id']?.toString(),
                          child: Text(c['title']?.toString() ?? 'Session', overflow: TextOverflow.ellipsis),
                        ))
                    .toList(),
                onChanged: (cid) {
                  if (cid == null) return;
                  final found = _conversations.where((c) => c['id']?.toString() == cid).firstOrNull;
                  setState(() {
                    _conversationId = cid;
                    _messages = (found?['messages'] as List<dynamic>? ?? []);
                  });
                },
              ),
            ),
          ],
          const Divider(height: RmapTokens.space4),
          if (_error != null)
            Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: Text(_error!, style: const TextStyle(color: RmapTokens.danger, fontSize: 11.5)),
            ),
          Expanded(
            child: project == null
                ? Center(
                    child: Text(
                      'Select or create a project to chat with full project-scoped context (ProjectSpec, documents, architecture).',
                      textAlign: TextAlign.center,
                      style: theme.textTheme.bodySmall,
                    ),
                  )
                : _messages.isEmpty
                    ? Center(
                        child: Text(
                          'Active Context: ${project.name} (${project.mode}).\nAsk about missing requirements, architecture, or next steps.',
                          textAlign: TextAlign.center,
                          style: theme.textTheme.bodySmall,
                        ),
                      )
                    : ListView.builder(
                        itemCount: _messages.length,
                        itemBuilder: (ctx, i) {
                          final m = _messages[i] as Map<String, dynamic>;
                          final isUser = m['role'] == 'USER';
                          final proposal = m['specProposal'] as Map<String, dynamic>?;
                          return Container(
                            margin: const EdgeInsets.only(bottom: 8),
                            padding: const EdgeInsets.all(8),
                            decoration: BoxDecoration(
                              color: isUser ? theme.colorScheme.primary.withValues(alpha: 0.08) : theme.scaffoldBackgroundColor,
                              borderRadius: BorderRadius.circular(RmapTokens.radiusSm),
                              border: Border.all(color: theme.dividerColor),
                            ),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(isUser ? 'You' : 'Rmap AI', style: theme.textTheme.labelSmall),
                                const SizedBox(height: 4),
                                Text(m['content']?.toString() ?? '', style: theme.textTheme.bodySmall),
                                if (proposal != null && proposal['status'] == 'PENDING_APPROVAL')
                                  Padding(
                                    padding: const EdgeInsets.only(top: 6),
                                    child: OutlinedButton(
                                      onPressed: () => _approveProposal(proposal['proposalId'].toString()),
                                      child: const Text('Approve Spec Proposal'),
                                    ),
                                  ),
                              ],
                            ),
                          );
                        },
                      ),
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _msgCtrl,
                  enabled: project != null && !_sending,
                  decoration: InputDecoration(
                    hintText: project == null ? 'Select a project first...' : 'Ask about ${project.name}...',
                  ),
                  onSubmitted: (_) => _send(),
                ),
              ),
              const SizedBox(width: 6),
              IconButton(
                icon: const Icon(Icons.send, size: 16),
                onPressed: project == null || _sending ? null : _send,
              ),
            ],
          ),
        ],
      ),
    );
  }
}
