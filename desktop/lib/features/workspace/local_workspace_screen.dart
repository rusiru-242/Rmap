import 'package:file_selector/file_selector.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/network/api_client.dart';
import '../../core/theme/rmap_tokens.dart';
import '../auth/auth_controller.dart';
import 'local_workspace_security.dart';

final activeWorkspaceScanProvider = StateProvider<LocalWorkspaceScanResult?>((ref) => null);

class LocalWorkspaceScreen extends ConsumerStatefulWidget {
  const LocalWorkspaceScreen({super.key});

  @override
  ConsumerState<LocalWorkspaceScreen> createState() => _LocalWorkspaceScreenState();
}

class _LocalWorkspaceScreenState extends ConsumerState<LocalWorkspaceScreen> {
  bool _scanning = false;
  bool _syncing = false;
  String? _statusMessage;
  String? _errorMessage;

  Future<void> _selectAndScanFolder() async {
    setState(() {
      _errorMessage = null;
      _statusMessage = null;
    });
    final selectedDir = await getDirectoryPath(confirmButtonText: 'Authorize Workspace Folder');
    if (selectedDir == null) return;

    setState(() => _scanning = true);
    try {
      final result = await LocalWorkspaceSecurity.scanReadOnly(selectedDir);
      ref.read(activeWorkspaceScanProvider.notifier).state = result;
      setState(() {
        _scanning = false;
        _statusMessage =
            'Read-only scan complete: ${result.indexedFileCount} files indexed, ${result.excludedSensitiveFileCount} sensitive files excluded.';
      });
    } catch (e) {
      setState(() {
        _scanning = false;
        _errorMessage = e.toString();
      });
    }
  }

  Future<void> _syncMetadataToBackend(LocalWorkspaceScanResult scan) async {
    setState(() {
      _syncing = true;
      _errorMessage = null;
      _statusMessage = null;
    });
    try {
      await ref.read(apiClientProvider).post('/api/v1/workspaces', {
        'displayName': scan.displayName,
        'pathHash': scan.pathHash,
        'detectedStacks': scan.detectedStacks,
        'indexedFileCount': scan.indexedFileCount,
        'excludedSensitiveFileCount': scan.excludedSensitiveFileCount,
      });
      setState(() {
        _syncing = false;
        _statusMessage = 'Workspace metadata (hashed path only, zero raw secrets) registered with backend.';
      });
    } on ApiClientException catch (e) {
      setState(() {
        _syncing = false;
        _errorMessage = e.message;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final scan = ref.watch(activeWorkspaceScanProvider);
    final auth = ref.watch(authControllerProvider);
    final theme = Theme.of(context);

    return ListView(
      padding: const EdgeInsets.all(RmapTokens.space6),
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Secure Local Workspace', style: theme.textTheme.headlineSmall),
                const SizedBox(height: 4),
                Text(
                  'Explicit user folder authorization with canonical path boundaries and sensitive-file exclusion.',
                  style: theme.textTheme.bodySmall,
                ),
              ],
            ),
            FilledButton.icon(
              onPressed: _scanning ? null : _selectAndScanFolder,
              icon: const Icon(Icons.folder_open_outlined, size: 16),
              label: Text(_scanning ? 'Scanning...' : 'Select Local Folder'),
            ),
          ],
        ),
        const SizedBox(height: RmapTokens.space4),
        if (_errorMessage != null)
          Card(
            child: Padding(
              padding: const EdgeInsets.all(RmapTokens.space3),
              child: Text(_errorMessage!, style: const TextStyle(color: RmapTokens.danger, fontSize: 12.5)),
            ),
          ),
        if (_statusMessage != null)
          Card(
            child: Padding(
              padding: const EdgeInsets.all(RmapTokens.space3),
              child: Text(_statusMessage!, style: const TextStyle(color: RmapTokens.success, fontSize: 12.5)),
            ),
          ),
        const SizedBox(height: RmapTokens.space4),
        if (scan == null)
          Card(
            child: Padding(
              padding: const EdgeInsets.all(RmapTokens.space6),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('No local workspace authorized', style: theme.textTheme.titleMedium),
                  const SizedBox(height: RmapTokens.space2),
                  Text(
                    'Rmap never accesses your filesystem without explicit folder selection. '
                    'Files matching .env, *.pem, *.key, *secret*, or *credential* are automatically excluded from AI context.',
                    style: theme.textTheme.bodySmall,
                  ),
                ],
              ),
            ),
          )
        else ...[
          Card(
            child: Padding(
              padding: const EdgeInsets.all(RmapTokens.space4),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(scan.displayName, style: theme.textTheme.titleMedium),
                          const SizedBox(height: 2),
                          Text(scan.rootPath, style: theme.textTheme.bodySmall),
                        ],
                      ),
                      if (auth.isAuthenticated)
                        OutlinedButton(
                          onPressed: _syncing || (auth.entitlement?.isReadOnly ?? false)
                              ? null
                              : () => _syncMetadataToBackend(scan),
                          child: Text(_syncing ? 'Syncing...' : 'Sync Metadata'),
                        ),
                    ],
                  ),
                  const Divider(height: RmapTokens.space5),
                  Wrap(
                    spacing: RmapTokens.space4,
                    runSpacing: RmapTokens.space2,
                    children: [
                      _MetricChip(label: 'Mode', value: 'Read-Only'),
                      _MetricChip(label: 'Indexed Files', value: '${scan.indexedFileCount}'),
                      _MetricChip(label: 'Sensitive Excluded', value: '${scan.excludedSensitiveFileCount}'),
                      _MetricChip(
                        label: 'Detected Stacks',
                        value: scan.detectedStacks.isEmpty ? 'Generic / Undetected' : scan.detectedStacks.join(', '),
                      ),
                    ],
                  ),
                  const SizedBox(height: RmapTokens.space4),
                  Text('Sample safe relative paths (${scan.sampleRelativePaths.length} shown)', style: theme.textTheme.labelSmall),
                  const SizedBox(height: RmapTokens.space2),
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(RmapTokens.space3),
                    decoration: BoxDecoration(
                      color: theme.scaffoldBackgroundColor,
                      borderRadius: BorderRadius.circular(RmapTokens.radiusSm),
                      border: Border.all(color: theme.dividerColor),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: scan.sampleRelativePaths
                          .take(15)
                          .map((rel) => Text(rel, style: const TextStyle(fontFamily: 'Consolas', fontSize: 12)))
                          .toList(),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ],
    );
  }
}

class _MetricChip extends StatelessWidget {
  final String label;
  final String value;

  const _MetricChip({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(RmapTokens.radiusSm),
        border: Border.all(color: theme.dividerColor),
      ),
      child: RichText(
        text: TextSpan(
          style: theme.textTheme.bodySmall,
          children: [
            TextSpan(text: '$label: '),
            TextSpan(text: value, style: TextStyle(fontWeight: FontWeight.w600, color: theme.colorScheme.onSurface)),
          ],
        ),
      ),
    );
  }
}
