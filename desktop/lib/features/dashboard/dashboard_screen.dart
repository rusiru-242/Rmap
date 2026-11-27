import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/theme/rmap_tokens.dart';
import '../auth/auth_controller.dart';
import '../projects/project_state.dart';
import '../workspace/local_workspace_screen.dart';

class DashboardScreen extends ConsumerWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authControllerProvider);
    final projects = ref.watch(projectsControllerProvider);
    final activeProject = projects.activeProject;
    final scan = ref.watch(activeWorkspaceScanProvider);
    final theme = Theme.of(context);

    return ListView(
      padding: const EdgeInsets.all(RmapTokens.space6),
      children: [
        Text('Workspace Overview', style: theme.textTheme.headlineSmall),
        const SizedBox(height: 4),
        Text(
          'Universal Project Intelligence, Versioned ProjectSpec, 4 Internal AI Agents, and Secure Local Workspace.',
          style: theme.textTheme.bodySmall,
        ),
        const SizedBox(height: RmapTokens.space5),
        Wrap(
          spacing: RmapTokens.space4,
          runSpacing: RmapTokens.space4,
          children: [
            SizedBox(
              width: 280,
              child: _SummaryCard(
                title: 'Active Project',
                value: activeProject?.name ?? 'None selected',
                subtitle: activeProject != null
                    ? '${activeProject.mode} · Spec v${activeProject.activeSpecVersion}'
                    : 'Create or open a project to manage specs, documents, and AI agents',
                actionLabel: activeProject != null ? 'Open Spec' : 'My Projects',
                onAction: () => context.go(activeProject != null ? '/discovery/requirements' : '/projects'),
              ),
            ),
            SizedBox(
              width: 280,
              child: _SummaryCard(
                title: 'AI Agent Orchestrator',
                value: '4 Specialized Agents',
                subtitle: 'Discovery, Architect, Engineering, and Quality & Delivery via backend Gemini LlmProvider',
                actionLabel: 'Agent Manager',
                onAction: () => context.go('/engineering/agents'),
              ),
            ),
            SizedBox(
              width: 280,
              child: _SummaryCard(
                title: 'Local Repository',
                value: scan?.displayName ?? 'Not linked',
                subtitle: scan != null
                    ? '${scan.indexedFileCount} safe files · ${scan.excludedSensitiveFileCount} secrets excluded'
                    : 'Select a local folder with read-only boundary enforcement',
                actionLabel: 'Open Explorer',
                onAction: () => context.go('/workspace/local'),
              ),
            ),
            SizedBox(
              width: 280,
              child: _SummaryCard(
                title: 'Account & Entitlement',
                value: auth.entitlement?.status ?? 'Authenticated',
                subtitle: auth.entitlement != null
                    ? 'Plan: ${auth.entitlement!.planCode} · Access: ${auth.entitlement!.accessLevel}'
                    : (auth.user?.email ?? 'Sign in to sync'),
                actionLabel: 'Subscription',
                onAction: () => context.go('/account/subscription'),
              ),
            ),
          ],
        ),
      ],
    );
  }
}

class _SummaryCard extends StatelessWidget {
  final String title;
  final String value;
  final String subtitle;
  final String actionLabel;
  final VoidCallback onAction;

  const _SummaryCard({
    required this.title,
    required this.value,
    required this.subtitle,
    required this.actionLabel,
    required this.onAction,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(RmapTokens.space4),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title, style: theme.textTheme.labelSmall),
            const SizedBox(height: RmapTokens.space2),
            Text(value, style: theme.textTheme.titleMedium, overflow: TextOverflow.ellipsis),
            const SizedBox(height: 6),
            Text(subtitle, style: theme.textTheme.bodySmall),
            const SizedBox(height: RmapTokens.space3),
            OutlinedButton(onPressed: onAction, child: Text(actionLabel)),
          ],
        ),
      ),
    );
  }
}
