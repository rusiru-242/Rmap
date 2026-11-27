import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/theme/rmap_tokens.dart';
import '../auth/auth_controller.dart';
import '../workspace/local_workspace_screen.dart';

class DashboardScreen extends ConsumerWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authControllerProvider);
    final scan = ref.watch(activeWorkspaceScanProvider);
    final theme = Theme.of(context);

    return ListView(
      padding: const EdgeInsets.all(RmapTokens.space6),
      children: [
        Text('Workspace Overview', style: theme.textTheme.headlineSmall),
        const SizedBox(height: 4),
        Text(
          'Phase 1 Core Platform active — Authentication, Entitlements, Design System, and Secure Local Workspace.',
          style: theme.textTheme.bodySmall,
        ),
        const SizedBox(height: RmapTokens.space5),
        Row(
          children: [
            Expanded(
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
            const SizedBox(width: RmapTokens.space4),
            Expanded(
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
            const SizedBox(width: RmapTokens.space4),
            Expanded(
              child: _SummaryCard(
                title: 'AI Engineering Agents',
                value: 'Phase 2',
                subtitle: 'Discovery, Architect, Engineering, and Quality agents via backend Gemini LlmProvider',
                actionLabel: 'Settings',
                onAction: () => context.go('/account/settings'),
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
            Text(value, style: theme.textTheme.titleMedium),
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
