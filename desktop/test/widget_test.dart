import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:path/path.dart' as p;
import 'package:rmap_desktop/app.dart';
import 'package:rmap_desktop/core/navigation/nav_registry.dart';
import 'package:rmap_desktop/core/theme/rmap_theme.dart';
import 'package:rmap_desktop/features/projects/projects_screens.dart';
import 'package:rmap_desktop/features/settings/account_screens.dart';
import 'package:rmap_desktop/features/shell/rmap_shell.dart';
import 'package:rmap_desktop/features/workspace/local_execution_engine.dart';
import 'package:rmap_desktop/features/workspace/local_workspace_screen.dart';
import 'package:rmap_desktop/features/workspace/local_workspace_security.dart';

void main() {
  group('LocalWorkspaceSecurity', () {
    test('blocks directory traversal outside workspace root', () {
      final root = p.join(Directory.systemTemp.path, 'rmap_root_test');
      expect(
        () => LocalWorkspaceSecurity.resolveSafePath(root, p.join('..', 'outside.txt')),
        throwsA(isA<WorkspaceBoundaryException>()),
      );
      final inside = LocalWorkspaceSecurity.resolveSafePath(root, p.join('src', 'main.dart'));
      expect(p.isWithin(p.canonicalize(root), inside), isTrue);
    });

    test('excludes sensitive files while allowing .env.example', () {
      expect(LocalWorkspaceSecurity.isSensitiveFile('.env'), isTrue);
      expect(LocalWorkspaceSecurity.isSensitiveFile('.env.production'), isTrue);
      expect(LocalWorkspaceSecurity.isSensitiveFile('certs/server.pem'), isTrue);
      expect(LocalWorkspaceSecurity.isSensitiveFile('keys/private.key'), isTrue);
      expect(LocalWorkspaceSecurity.isSensitiveFile('config/service-account-prod.json'), isTrue);
      expect(LocalWorkspaceSecurity.isSensitiveFile('.env.example'), isFalse);
      expect(LocalWorkspaceSecurity.isSensitiveFile('src/app.dart'), isFalse);
    });

    test('scanReadOnly detects stacks and excludes sensitive files', () async {
      final tempDir = await Directory.systemTemp.createTemp('rmap_scan_test_');
      try {
        await File(p.join(tempDir.path, 'pom.xml')).writeAsString('<project/>');
        await File(p.join(tempDir.path, '.env')).writeAsString('SECRET=123');
        await File(p.join(tempDir.path, '.env.example')).writeAsString('SECRET=');
        await File(p.join(tempDir.path, 'README.md')).writeAsString('# Demo');

        final result = await LocalWorkspaceSecurity.scanReadOnly(tempDir.path);
        expect(result.indexedFileCount, 3);
        expect(result.excludedSensitiveFileCount, 1);
        expect(result.detectedStacks, contains('Java / JVM'));
        expect(result.sampleRelativePaths, isNot(contains('.env')));
      } finally {
        await tempDir.delete(recursive: true);
      }
    });
  });

  group('RmapNavRegistry', () {
    test('filters project-specific navigation by active project mode', () {
      final generalIds = RmapNavRegistry.visibleItems(ProjectContextMode.general).map((i) => i.id).toSet();
      final academicIds = RmapNavRegistry.visibleItems(ProjectContextMode.academic).map((i) => i.id).toSet();
      final mlIds = RmapNavRegistry.visibleItems(ProjectContextMode.ml).map((i) => i.id).toSet();

      expect(generalIds.contains('academic'), isFalse);
      expect(generalIds.contains('ml'), isFalse);
      expect(academicIds.contains('academic'), isTrue);
      expect(academicIds.contains('viva'), isTrue);
      expect(mlIds.contains('ml'), isTrue);
    });
  });

  testWidgets('RmapDesktopApp renders authentication screen when unauthenticated', (tester) async {
    await tester.pumpWidget(const ProviderScope(child: RmapDesktopApp()));
    await tester.pumpAndSettle();

    expect(find.text('Rmap Workspace'), findsOneWidget);
    expect(find.text('Sign in to Rmap'), findsOneWidget);
  });

  testWidgets('RmapShell renders sidebar navigation, AI panel, command palette, and theme status bar', (tester) async {
    tester.view.physicalSize = const Size(1440, 900);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);

    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp(
          theme: RmapTheme.light(),
          darkTheme: RmapTheme.dark(),
          home: const RmapShell(
            currentLocation: '/workspace/local',
            child: LocalWorkspaceScreen(),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('WORKSPACE'), findsOneWidget);
    expect(find.text('Project-Aware AI Assistant'), findsOneWidget);
    expect(find.text('Secure Local Workspace'), findsOneWidget);
    expect(find.text('Select Local Folder'), findsOneWidget);

    await tester.tap(find.text('Command Palette  Ctrl+K'));
    await tester.pumpAndSettle();
    expect(find.text('Jump to workspace view...'), findsOneWidget);
  });

  testWidgets('SettingsScreen switches theme mode between System, Light, and Dark', (tester) async {
    final container = ProviderContainer();
    addTearDown(container.dispose);

    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: MaterialApp(
          theme: RmapTheme.light(),
          darkTheme: RmapTheme.dark(),
          home: const Scaffold(body: SettingsScreen()),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(container.read(themeModeProvider), ThemeMode.system);
    await tester.tap(find.text('Dark'));
    await tester.pumpAndSettle();
    expect(container.read(themeModeProvider), ThemeMode.dark);
  });

  testWidgets('CreateProjectWizardScreen adapts dynamic fields by project mode', (tester) async {
    tester.view.physicalSize = const Size(1440, 900);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);

    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp(
          theme: RmapTheme.light(),
          home: const Scaffold(body: CreateProjectWizardScreen()),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Create Project Wizard'), findsOneWidget);
    expect(find.text('Budget'), findsNothing);

    await tester.tap(find.text('CLIENT'));
    await tester.pumpAndSettle();
    expect(find.text('Budget'), findsOneWidget);
  });

  group('LocalExecutionEngine security', () {
    test('blocks unallowlisted executables and shell metacharacters', () {
      expect(
        () => LocalExecutionEngine.validateCommand('powershell', ['-Command', 'dir']),
        throwsA(isA<ExecutionSecurityException>()),
      );
      expect(
        () => LocalExecutionEngine.validateCommand('git', ['status; rm -rf /']),
        throwsA(isA<ExecutionSecurityException>()),
      );
      expect(
        () => LocalExecutionEngine.validateCommand('git', ['status', '--short']),
        returnsNormally,
      );
    });

    test('redacts secret-like output values', () {
      final clean = LocalExecutionEngine.redactOutput('API_KEY=sk-secret-value ok');
      expect(clean, contains('API_KEY=[REDACTED]'));
      expect(clean, isNot(contains('sk-secret-value')));
    });
  });
}
