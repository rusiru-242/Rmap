import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/network/api_client.dart';
import '../../core/theme/rmap_theme.dart';
import '../../core/theme/rmap_tokens.dart';
import '../auth/auth_controller.dart';

class ProfileScreen extends ConsumerWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authControllerProvider);
    final theme = Theme.of(context);

    return ListView(
      padding: const EdgeInsets.all(RmapTokens.space6),
      children: [
        Text('Profile', style: theme.textTheme.headlineSmall),
        const SizedBox(height: RmapTokens.space4),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(RmapTokens.space4),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(auth.user?.fullName ?? 'Rmap User', style: theme.textTheme.titleMedium),
                const SizedBox(height: 4),
                Text(auth.user?.email ?? 'Not signed in', style: theme.textTheme.bodySmall),
                const SizedBox(height: 4),
                Text('Role: ${auth.user?.role ?? 'USER'}', style: theme.textTheme.bodySmall),
                const SizedBox(height: RmapTokens.space4),
                OutlinedButton.icon(
                  onPressed: () => ref.read(authControllerProvider.notifier).logout(),
                  icon: const Icon(Icons.logout, size: 15),
                  label: const Text('Sign out'),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class SubscriptionScreen extends ConsumerWidget {
  const SubscriptionScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final ent = ref.watch(authControllerProvider).entitlement;
    final theme = Theme.of(context);

    return ListView(
      padding: const EdgeInsets.all(RmapTokens.space6),
      children: [
        Text('Subscription & Trial', style: theme.textTheme.headlineSmall),
        const SizedBox(height: 4),
        Text(
          'Entitlements are enforced exclusively by the Rmap backend. Expired trials automatically transition to read-only access.',
          style: theme.textTheme.bodySmall,
        ),
        const SizedBox(height: RmapTokens.space4),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(RmapTokens.space4),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Current Plan: ${ent?.planCode ?? 'TRIAL_72H'}', style: theme.textTheme.titleMedium),
                const SizedBox(height: 6),
                Text('Status: ${ent?.status ?? 'TRIALING'} · Access: ${ent?.accessLevel ?? 'FULL_ACCESS'}', style: theme.textTheme.bodySmall),
                const SizedBox(height: 6),
                Text(
                  ent?.paymentProviderConfigured == true
                      ? 'Payment provider is configured.'
                      : 'Payment checkout is disabled until a live payment provider is configured on the backend (no simulated charges).',
                  style: theme.textTheme.bodySmall,
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class SettingsScreen extends ConsumerStatefulWidget {
  const SettingsScreen({super.key});

  @override
  ConsumerState<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends ConsumerState<SettingsScreen> {
  late final TextEditingController _urlCtrl;

  @override
  void initState() {
    super.initState();
    _urlCtrl = TextEditingController(text: ref.read(apiBaseUrlProvider));
  }

  @override
  void dispose() {
    _urlCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final themeMode = ref.watch(themeModeProvider);
    final theme = Theme.of(context);

    return ListView(
      padding: const EdgeInsets.all(RmapTokens.space6),
      children: [
        Text('Settings', style: theme.textTheme.headlineSmall),
        const SizedBox(height: RmapTokens.space4),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(RmapTokens.space4),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Appearance', style: theme.textTheme.titleSmall),
                const SizedBox(height: RmapTokens.space3),
                SegmentedButton<ThemeMode>(
                  segments: const [
                    ButtonSegment(value: ThemeMode.system, label: Text('System')),
                    ButtonSegment(value: ThemeMode.light, label: Text('Light')),
                    ButtonSegment(value: ThemeMode.dark, label: Text('Dark')),
                  ],
                  selected: {themeMode},
                  onSelectionChanged: (s) => ref.read(themeModeProvider.notifier).state = s.first,
                ),
                const Divider(height: RmapTokens.space6),
                Text('Backend API Base URL', style: theme.textTheme.titleSmall),
                const SizedBox(height: RmapTokens.space2),
                Row(
                  children: [
                    Expanded(child: TextField(controller: _urlCtrl)),
                    const SizedBox(width: RmapTokens.space2),
                    FilledButton(
                      onPressed: () {
                        ref.read(apiBaseUrlProvider.notifier).state = _urlCtrl.text.trim();
                      },
                      child: const Text('Save'),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class RoadmapModulePlaceholder extends StatelessWidget {
  final String title;
  final int phase;

  const RoadmapModulePlaceholder({super.key, required this.title, required this.phase});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Center(
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 460),
        child: Card(
          child: Padding(
            padding: const EdgeInsets.all(RmapTokens.space6),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: theme.textTheme.headlineSmall),
                const SizedBox(height: RmapTokens.space2),
                Text(
                  'Scheduled for Phase $phase in the Rmap consolidated implementation roadmap. '
                  'Navigation routing and workspace context boundaries are wired in Phase 1 without fake functionality.',
                  style: theme.textTheme.bodySmall,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
