import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/navigation/nav_registry.dart';
import '../../core/theme/rmap_theme.dart';
import '../../core/theme/rmap_tokens.dart';
import '../auth/auth_controller.dart';
import '../workspace/local_workspace_screen.dart';

final activeProjectModeProvider = StateProvider<ProjectContextMode>((ref) => ProjectContextMode.general);
final sidebarCollapsedProvider = StateProvider<bool>((ref) => false);
final aiPanelVisibleProvider = StateProvider<bool>((ref) => true);
final aiPanelWidthProvider = StateProvider<double>((ref) => RmapTokens.aiPanelDefaultWidth);

class RmapShell extends ConsumerWidget {
  final Widget child;
  final String currentLocation;

  const RmapShell({
    super.key,
    required this.child,
    required this.currentLocation,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final collapsed = ref.watch(sidebarCollapsedProvider);
    final aiVisible = ref.watch(aiPanelVisibleProvider);
    final aiWidth = ref.watch(aiPanelWidthProvider);
    final projectMode = ref.watch(activeProjectModeProvider);
    final auth = ref.watch(authControllerProvider);
    final scan = ref.watch(activeWorkspaceScanProvider);
    final theme = Theme.of(context);

    return CallbackShortcuts(
      bindings: {
        const SingleActivator(LogicalKeyboardKey.keyK, control: true): () =>
            _openCommandPalette(context, ref, projectMode),
        const SingleActivator(LogicalKeyboardKey.keyB, control: true): () =>
            ref.read(sidebarCollapsedProvider.notifier).state = !collapsed,
      },
      child: Focus(
        autofocus: true,
        child: Scaffold(
          body: Column(
            children: [
              _TopBar(
                projectMode: projectMode,
                onToggleSidebar: () => ref.read(sidebarCollapsedProvider.notifier).state = !collapsed,
                onToggleAiPanel: () => ref.read(aiPanelVisibleProvider.notifier).state = !aiVisible,
                onOpenCommandPalette: () => _openCommandPalette(context, ref, projectMode),
                onChangeMode: (m) => ref.read(activeProjectModeProvider.notifier).state = m,
              ),
              Expanded(
                child: Row(
                  children: [
                    AnimatedContainer(
                      duration: const Duration(milliseconds: 140),
                      width: collapsed ? RmapTokens.sidebarCollapsedWidth : RmapTokens.sidebarExpandedWidth,
                      decoration: BoxDecoration(
                        color: theme.colorScheme.surface,
                        border: Border(right: BorderSide(color: theme.dividerColor)),
                      ),
                      child: _SidebarNav(
                        collapsed: collapsed,
                        currentLocation: currentLocation,
                        mode: projectMode,
                      ),
                    ),
                    Expanded(
                      child: Column(
                        children: [
                          if (auth.entitlement?.isReadOnly == true)
                            Container(
                              width: double.infinity,
                              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                              color: RmapTokens.warning.withValues(alpha: 0.14),
                              child: const Text(
                                'Read-Only Mode: Your 72-hour trial or subscription has expired. Existing workspace data remains readable.',
                                style: TextStyle(fontSize: 12, color: RmapTokens.warning, fontWeight: FontWeight.w500),
                              ),
                            ),
                          Expanded(child: child),
                        ],
                      ),
                    ),
                    if (aiVisible) ...[
                      MouseRegion(
                        cursor: SystemMouseCursors.resizeColumn,
                        child: GestureDetector(
                          onHorizontalDragUpdate: (details) {
                            final next = (aiWidth - details.delta.dx)
                                .clamp(RmapTokens.aiPanelMinWidth, RmapTokens.aiPanelMaxWidth);
                            ref.read(aiPanelWidthProvider.notifier).state = next;
                          },
                          child: Container(
                            width: 4,
                            color: theme.dividerColor,
                          ),
                        ),
                      ),
                      SizedBox(
                        width: aiWidth,
                        child: const _AiAssistantSidePanel(),
                      ),
                    ],
                  ],
                ),
              ),
              _StatusBar(
                workspaceName: scan?.displayName ?? 'No local folder',
                entitlement: auth.entitlement,
                themeMode: ref.watch(themeModeProvider),
                onCycleTheme: () {
                  final current = ref.read(themeModeProvider);
                  final next = switch (current) {
                    ThemeMode.system => ThemeMode.dark,
                    ThemeMode.dark => ThemeMode.light,
                    ThemeMode.light => ThemeMode.system,
                  };
                  ref.read(themeModeProvider.notifier).state = next;
                },
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _openCommandPalette(BuildContext context, WidgetRef ref, ProjectContextMode mode) {
    final items = RmapNavRegistry.visibleItems(mode);
    showDialog<void>(
      context: context,
      builder: (ctx) => _CommandPaletteDialog(
        items: items,
        onSelect: (route) {
          Navigator.of(ctx).pop();
          context.go(route);
        },
      ),
    );
  }
}

class _TopBar extends StatelessWidget {
  final ProjectContextMode projectMode;
  final VoidCallback onToggleSidebar;
  final VoidCallback onToggleAiPanel;
  final VoidCallback onOpenCommandPalette;
  final ValueChanged<ProjectContextMode> onChangeMode;

  const _TopBar({
    required this.projectMode,
    required this.onToggleSidebar,
    required this.onToggleAiPanel,
    required this.onOpenCommandPalette,
    required this.onChangeMode,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      height: RmapTokens.topBarHeight,
      padding: const EdgeInsets.symmetric(horizontal: RmapTokens.space3),
      decoration: BoxDecoration(
        color: theme.colorScheme.surface,
        border: Border(bottom: BorderSide(color: theme.dividerColor)),
      ),
      child: Row(
        children: [
          IconButton(
            tooltip: 'Toggle Sidebar (Ctrl+B)',
            icon: const Icon(Icons.menu, size: 18),
            onPressed: onToggleSidebar,
            visualDensity: VisualDensity.compact,
          ),
          const SizedBox(width: RmapTokens.space2),
          Text('Rmap', style: theme.textTheme.titleMedium),
          const SizedBox(width: RmapTokens.space4),
          DropdownButtonHideUnderline(
            child: DropdownButton<ProjectContextMode>(
              value: projectMode,
              isDense: true,
              style: theme.textTheme.bodySmall,
              items: const [
                DropdownMenuItem(value: ProjectContextMode.general, child: Text('Project Mode: Individual / SaaS')),
                DropdownMenuItem(value: ProjectContextMode.academic, child: Text('Project Mode: Academic')),
                DropdownMenuItem(value: ProjectContextMode.team, child: Text('Project Mode: Team')),
                DropdownMenuItem(value: ProjectContextMode.ml, child: Text('Project Mode: Machine Learning')),
                DropdownMenuItem(value: ProjectContextMode.client, child: Text('Project Mode: Client')),
              ],
              onChanged: (v) {
                if (v != null) onChangeMode(v);
              },
            ),
          ),
          const Spacer(),
          OutlinedButton.icon(
            onPressed: onOpenCommandPalette,
            icon: const Icon(Icons.search, size: 14),
            label: const Text('Command Palette  Ctrl+K', style: TextStyle(fontSize: 11.5)),
            style: OutlinedButton.styleFrom(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
              minimumSize: const Size(0, 28),
            ),
          ),
          const SizedBox(width: RmapTokens.space2),
          IconButton(
            tooltip: 'Toggle AI Assistant Panel',
            icon: const Icon(Icons.auto_awesome_outlined, size: 17),
            onPressed: onToggleAiPanel,
            visualDensity: VisualDensity.compact,
          ),
        ],
      ),
    );
  }
}

class _SidebarNav extends StatelessWidget {
  final bool collapsed;
  final String currentLocation;
  final ProjectContextMode mode;

  const _SidebarNav({
    required this.collapsed,
    required this.currentLocation,
    required this.mode,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return ListView(
      padding: const EdgeInsets.symmetric(vertical: RmapTokens.space2),
      children: [
        for (final group in RmapNavRegistry.groups) ...[
          if (group.items.any((i) => i.isVisibleFor(mode))) ...[
            if (!collapsed)
              Padding(
                padding: const EdgeInsets.fromLTRB(12, 10, 12, 4),
                child: Text(group.title, style: theme.textTheme.labelSmall),
              ),
            for (final item in group.items.where((i) => i.isVisibleFor(mode)))
              _NavTile(
                item: item,
                collapsed: collapsed,
                selected: currentLocation == item.route,
                onTap: () => context.go(item.route),
              ),
          ],
        ],
      ],
    );
  }
}

class _NavTile extends StatelessWidget {
  final NavItemSpec item;
  final bool collapsed;
  final bool selected;
  final VoidCallback onTap;

  const _NavTile({
    required this.item,
    required this.collapsed,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final fg = selected ? theme.colorScheme.primary : theme.colorScheme.onSurface;
    return Tooltip(
      message: collapsed ? item.label : '',
      child: InkWell(
        onTap: onTap,
        child: Container(
          margin: const EdgeInsets.symmetric(horizontal: 6, vertical: 1),
          padding: EdgeInsets.symmetric(horizontal: collapsed ? 0 : 8, vertical: 7),
          decoration: BoxDecoration(
            color: selected ? theme.colorScheme.primary.withValues(alpha: 0.1) : Colors.transparent,
            borderRadius: BorderRadius.circular(RmapTokens.radiusSm),
          ),
          child: Row(
            mainAxisAlignment: collapsed ? MainAxisAlignment.center : MainAxisAlignment.start,
            children: [
              Icon(item.icon, size: 16, color: fg),
              if (!collapsed) ...[
                const SizedBox(width: 9),
                Expanded(
                  child: Text(
                    item.label,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(fontSize: 12.5, fontWeight: selected ? FontWeight.w600 : FontWeight.w400, color: fg),
                  ),
                ),
                if (item.phase > 1)
                  Text('P${item.phase}', style: theme.textTheme.labelSmall?.copyWith(fontSize: 10)),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class _AiAssistantSidePanel extends StatelessWidget {
  const _AiAssistantSidePanel();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
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
              Text('Project-Aware AI Assistant', style: theme.textTheme.titleSmall),
            ],
          ),
          const Divider(height: RmapTokens.space4),
          Expanded(
            child: Center(
              child: Text(
                'Google Gemini internal agents (Discovery, Architect, Engineering, Quality & Delivery) '
                'connect through the backend LlmProvider in Phase 2.\n\n'
                'No static fake AI responses are enabled in Phase 1.',
                textAlign: TextAlign.center,
                style: theme.textTheme.bodySmall,
              ),
            ),
          ),
          const TextField(
            enabled: false,
            decoration: InputDecoration(
              hintText: 'Available in Phase 2 (Gemini LlmProvider)',
            ),
          ),
        ],
      ),
    );
  }
}

class _StatusBar extends StatelessWidget {
  final String workspaceName;
  final EntitlementInfo? entitlement;
  final ThemeMode themeMode;
  final VoidCallback onCycleTheme;

  const _StatusBar({
    required this.workspaceName,
    required this.entitlement,
    required this.themeMode,
    required this.onCycleTheme,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final entText = entitlement == null
        ? 'Session: Local Shell'
        : '${entitlement!.planCode} (${entitlement!.status}${entitlement!.remainingTrialHours > 0 ? ' · ${entitlement!.remainingTrialHours}h left' : ''})';
    return Container(
      height: RmapTokens.statusBarHeight,
      padding: const EdgeInsets.symmetric(horizontal: RmapTokens.space3),
      decoration: BoxDecoration(
        color: theme.colorScheme.surface,
        border: Border(top: BorderSide(color: theme.dividerColor)),
      ),
      child: Row(
        children: [
          Icon(Icons.folder_outlined, size: 13, color: theme.textTheme.bodySmall?.color),
          const SizedBox(width: 6),
          Text(workspaceName, style: theme.textTheme.labelSmall),
          const SizedBox(width: RmapTokens.space4),
          Text(entText, style: theme.textTheme.labelSmall),
          const Spacer(),
          InkWell(
            onTap: onCycleTheme,
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 6),
              child: Text('Theme: ${themeMode.name}', style: theme.textTheme.labelSmall),
            ),
          ),
        ],
      ),
    );
  }
}

class _CommandPaletteDialog extends StatefulWidget {
  final List<NavItemSpec> items;
  final ValueChanged<String> onSelect;

  const _CommandPaletteDialog({required this.items, required this.onSelect});

  @override
  State<_CommandPaletteDialog> createState() => _CommandPaletteDialogState();
}

class _CommandPaletteDialogState extends State<_CommandPaletteDialog> {
  String _query = '';

  @override
  Widget build(BuildContext context) {
    final filtered = widget.items
        .where((i) =>
            i.label.toLowerCase().contains(_query.toLowerCase()) ||
            i.group.toLowerCase().contains(_query.toLowerCase()))
        .toList();

    return Dialog(
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 480, maxHeight: 420),
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.all(RmapTokens.space3),
              child: TextField(
                autofocus: true,
                decoration: const InputDecoration(
                  prefixIcon: Icon(Icons.search, size: 16),
                  hintText: 'Jump to workspace view...',
                ),
                onChanged: (v) => setState(() => _query = v),
              ),
            ),
            const Divider(height: 1),
            Expanded(
              child: ListView.builder(
                itemCount: filtered.length,
                itemBuilder: (ctx, idx) {
                  final item = filtered[idx];
                  return ListTile(
                    dense: true,
                    leading: Icon(item.icon, size: 16),
                    title: Text(item.label, style: const TextStyle(fontSize: 13)),
                    trailing: Text(item.group, style: Theme.of(context).textTheme.labelSmall),
                    onTap: () => widget.onSelect(item.route),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}
