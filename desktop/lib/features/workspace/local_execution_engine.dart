import 'dart:async';
import 'dart:io';

import 'package:path/path.dart' as p;
import 'local_workspace_security.dart';

class ExecutionSecurityException implements Exception {
  final String message;
  const ExecutionSecurityException(this.message);
  @override
  String toString() => message;
}

class LocalCommandResult {
  final String executable;
  final List<String> arguments;
  final int exitCode;
  final String stdout;
  final String stderr;
  final bool timedOut;

  const LocalCommandResult({
    required this.executable,
    required this.arguments,
    required this.exitCode,
    required this.stdout,
    required this.stderr,
    this.timedOut = false,
  });
}

class ToolVersionStatus {
  final String name;
  final String executable;
  final bool installed;
  final String versionOutput;
  final String officialSource;

  const ToolVersionStatus({
    required this.name,
    required this.executable,
    required this.installed,
    required this.versionOutput,
    required this.officialSource,
  });
}

/// Controlled local command execution engine for Rmap Desktop.
/// - Enforces strict executable allowlist
/// - Uses structured process arguments (runInShell: false — no shell concatenation)
/// - Blocks shell metacharacters in arguments
/// - Validates canonical workspace root working directory
/// - Enforces timeouts, output size caps, and secret redaction
class LocalExecutionEngine {
  static const int maxOutputChars = 65536;

  static const Set<String> allowlistedExecutables = {
    'git',
    'java',
    'mvn',
    'node',
    'npm',
    'flutter',
    'dart',
    'python',
    'pytest',
    'dotnet',
    'docker',
  };

  static final RegExp _unsafeArgPattern = RegExp(r'[;|&`><$\r\n]');
  static final RegExp _secretPattern = RegExp(
    r'(api[_-]?key|secret|password|bearer|token)\s*[:=]\s*\S+',
    caseSensitive: false,
  );

  static void validateCommand(String executable, List<String> arguments) {
    final base = p.basename(executable).toLowerCase().replaceAll('.exe', '').replaceAll('.cmd', '').replaceAll('.bat', '');
    if (!allowlistedExecutables.contains(base)) {
      throw ExecutionSecurityException('Executable "$executable" is not in Rmap\'s allowlist.');
    }
    for (final arg in arguments) {
      if (_unsafeArgPattern.hasMatch(arg)) {
        throw ExecutionSecurityException('Unsafe shell metacharacter rejected in argument: $arg');
      }
    }
  }

  static String redactOutput(String input) {
    final capped = input.length > maxOutputChars ? '${input.substring(0, maxOutputChars)}\n...[output truncated]' : input;
    return capped.replaceAllMapped(_secretPattern, (m) => '${m.group(1)}=[REDACTED]');
  }

  static Future<LocalCommandResult> runAllowlisted({
    required String executable,
    required List<String> arguments,
    required String workspaceRoot,
    Duration timeout = const Duration(seconds: 30),
  }) async {
    validateCommand(executable, arguments);
    final safeRoot = LocalWorkspaceSecurity.resolveSafePath(workspaceRoot, workspaceRoot);

    try {
      final result = await Process.run(
        executable,
        arguments,
        workingDirectory: safeRoot,
        runInShell: false,
      ).timeout(timeout);

      return LocalCommandResult(
        executable: executable,
        arguments: arguments,
        exitCode: result.exitCode,
        stdout: redactOutput(result.stdout.toString()),
        stderr: redactOutput(result.stderr.toString()),
      );
    } on TimeoutException {
      return LocalCommandResult(
        executable: executable,
        arguments: arguments,
        exitCode: -1,
        stdout: '',
        stderr: 'Command timed out after ${timeout.inSeconds}s.',
        timedOut: true,
      );
    } on ProcessException catch (e) {
      return LocalCommandResult(
        executable: executable,
        arguments: arguments,
        exitCode: -1,
        stdout: '',
        stderr: 'Process unavailable: ${e.message}',
      );
    }
  }

  static Future<List<ToolVersionStatus>> auditLocalEnvironment() async {
    const specs = [
      ('Git', 'git', ['--version'], 'https://git-scm.com/downloads'),
      ('Java JDK', 'java', ['-version'], 'https://adoptium.net/'),
      ('Flutter', 'flutter', ['--version'], 'https://docs.flutter.dev/get-started/install'),
      ('Dart', 'dart', ['--version'], 'https://dart.dev/get-dart'),
      ('Node.js', 'node', ['-v'], 'https://nodejs.org/'),
      ('Python', 'python', ['--version'], 'https://www.python.org/downloads/'),
      ('Docker', 'docker', ['--version'], 'https://www.docker.com/products/docker-desktop/'),
      ('.NET SDK', 'dotnet', ['--version'], 'https://dotnet.microsoft.com/download'),
    ];

    final results = <ToolVersionStatus>[];
    for (final (name, exe, args, url) in specs) {
      try {
        final res = await Process.run(exe, args, runInShell: false).timeout(const Duration(seconds: 6));
        final combined = ('${res.stdout}\n${res.stderr}').trim();
        final firstLine = combined.split('\n').firstWhere((l) => l.trim().isNotEmpty, orElse: () => 'Installed');
        results.add(ToolVersionStatus(
          name: name,
          executable: exe,
          installed: res.exitCode == 0,
          versionOutput: res.exitCode == 0 ? firstLine.trim() : 'Not installed',
          officialSource: url,
        ));
      } catch (_) {
        results.add(ToolVersionStatus(
          name: name,
          executable: exe,
          installed: false,
          versionOutput: 'Not installed',
          officialSource: url,
        ));
      }
    }
    return results;
  }

  static Future<Map<String, String>> inspectGitWorkspace(String workspaceRoot) async {
    final branch = await runAllowlisted(executable: 'git', arguments: ['branch', '--show-current'], workspaceRoot: workspaceRoot);
    final status = await runAllowlisted(executable: 'git', arguments: ['status', '--short'], workspaceRoot: workspaceRoot);
    final log = await runAllowlisted(executable: 'git', arguments: ['log', '-n', '8', '--oneline'], workspaceRoot: workspaceRoot);
    return {
      'branch': branch.exitCode == 0 ? branch.stdout.trim() : 'Not a Git repository',
      'status': status.exitCode == 0 ? (status.stdout.trim().isEmpty ? 'Clean working tree' : status.stdout.trim()) : status.stderr.trim(),
      'recentCommits': log.exitCode == 0 ? log.stdout.trim() : '',
    };
  }
}
