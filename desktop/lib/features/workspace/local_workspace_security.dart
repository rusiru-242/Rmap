import 'dart:convert';
import 'dart:io';

import 'package:crypto/crypto.dart';
import 'package:path/path.dart' as p;

class WorkspaceBoundaryException implements Exception {
  final String message;
  const WorkspaceBoundaryException(this.message);
  @override
  String toString() => message;
}

class LocalWorkspaceScanResult {
  final String rootPath;
  final String displayName;
  final String pathHash;
  final int indexedFileCount;
  final int excludedSensitiveFileCount;
  final List<String> detectedStacks;
  final List<String> sampleRelativePaths;
  final bool truncatedByLimit;

  const LocalWorkspaceScanResult({
    required this.rootPath,
    required this.displayName,
    required this.pathHash,
    required this.indexedFileCount,
    required this.excludedSensitiveFileCount,
    required this.detectedStacks,
    required this.sampleRelativePaths,
    required this.truncatedByLimit,
  });
}

/// Enforces Rmap's local execution security policy:
/// - Canonical path boundary validation (blocks traversal outside workspace root)
/// - Sensitive file & credential exclusion from AI context
/// - Read-only, bounded scanning with file & depth caps
class LocalWorkspaceSecurity {
  static const int maxFiles = 2500;
  static const int maxDepth = 12;

  static const Set<String> ignoredDirectories = {
    '.git',
    'node_modules',
    'target',
    'build',
    '.dart_tool',
    '.venv',
    'venv',
    '__pycache__',
    'dist',
    '.idea',
    '.gradle',
  };

  static const Set<String> sensitiveExtensions = {
    '.pem',
    '.key',
    '.p12',
    '.pfx',
    '.jks',
    '.keystore',
    '.kdbx',
  };

  /// Verifies [candidatePath] resolves strictly inside [workspaceRoot].
  static String resolveSafePath(String workspaceRoot, String candidatePath) {
    final canonicalRoot = p.canonicalize(workspaceRoot);
    final resolved = p.canonicalize(
      p.isAbsolute(candidatePath) ? candidatePath : p.join(canonicalRoot, candidatePath),
    );
    if (resolved != canonicalRoot && !p.isWithin(canonicalRoot, resolved)) {
      throw WorkspaceBoundaryException(
        'Path escapes authorized workspace boundary: $candidatePath',
      );
    }
    return resolved;
  }

  /// Returns true if a file must NEVER be sent to AI context or indexed in content summaries.
  static bool isSensitiveFile(String relativePath) {
    final base = p.basename(relativePath).toLowerCase();
    final ext = p.extension(base).toLowerCase();

    if (base == '.env.example' || base == '.env.template' || base == '.env.sample') {
      return false;
    }
    if (base == '.env' || base.startsWith('.env.')) {
      return true;
    }
    if (sensitiveExtensions.contains(ext)) {
      return true;
    }
    if (base == 'id_rsa' || base == 'id_ed25519' || base == 'id_ecdsa') {
      return true;
    }
    if (base.contains('secret') || base.contains('credential') || base.startsWith('service-account')) {
      return true;
    }
    return false;
  }

  static String hashWorkspacePath(String rootPath) {
    final canonical = p.canonicalize(rootPath);
    return sha256.convert(utf8.encode(canonical)).toString();
  }

  /// Performs a strictly read-only, bounded scan of [rootPath].
  static Future<LocalWorkspaceScanResult> scanReadOnly(String rootPath) async {
    final canonicalRoot = p.canonicalize(rootPath);
    final rootDir = Directory(canonicalRoot);
    if (!await rootDir.exists()) {
      throw WorkspaceBoundaryException('Workspace folder does not exist: $canonicalRoot');
    }

    int indexed = 0;
    int excludedSensitive = 0;
    bool truncated = false;
    final stacks = <String>{};
    final samples = <String>[];

    Future<void> walk(Directory dir, int depth) async {
      if (depth > maxDepth || indexed >= maxFiles) {
        if (indexed >= maxFiles) truncated = true;
        return;
      }

      List<FileSystemEntity> entries;
      try {
        entries = await dir.list(followLinks: false).toList();
      } on FileSystemException {
        return;
      }

      for (final entity in entries) {
        if (indexed >= maxFiles) {
          truncated = true;
          return;
        }
        final name = p.basename(entity.path);
        if (entity is Directory) {
          if (ignoredDirectories.contains(name)) continue;
          resolveSafePath(canonicalRoot, entity.path);
          await walk(entity, depth + 1);
        } else if (entity is File) {
          final safe = resolveSafePath(canonicalRoot, entity.path);
          final rel = p.relative(safe, from: canonicalRoot);
          if (isSensitiveFile(rel)) {
            excludedSensitive++;
            continue;
          }
          indexed++;
          if (samples.length < 40) {
            samples.add(rel);
          }
          _detectStackFromFile(name.toLowerCase(), stacks);
        }
      }
    }

    await walk(rootDir, 0);

    return LocalWorkspaceScanResult(
      rootPath: canonicalRoot,
      displayName: p.basename(canonicalRoot),
      pathHash: hashWorkspacePath(canonicalRoot),
      indexedFileCount: indexed,
      excludedSensitiveFileCount: excludedSensitive,
      detectedStacks: stacks.toList()..sort(),
      sampleRelativePaths: samples,
      truncatedByLimit: truncated,
    );
  }

  static void _detectStackFromFile(String fileName, Set<String> stacks) {
    if (fileName == 'pom.xml' || fileName == 'build.gradle' || fileName == 'build.gradle.kts') {
      stacks.add('Java / JVM');
    } else if (fileName == 'pubspec.yaml') {
      stacks.add('Flutter / Dart');
    } else if (fileName == 'package.json') {
      stacks.add('Node.js / TypeScript');
    } else if (fileName == 'requirements.txt' || fileName == 'pyproject.toml') {
      stacks.add('Python');
    } else if (fileName == 'cargo.toml') {
      stacks.add('Rust');
    } else if (fileName == 'go.mod') {
      stacks.add('Go');
    } else if (fileName == 'docker-compose.yml' || fileName == 'dockerfile') {
      stacks.add('Docker');
    }
  }
}
