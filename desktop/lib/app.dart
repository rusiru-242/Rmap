import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'core/routing/app_router.dart';
import 'core/theme/rmap_theme.dart';

class RmapDesktopApp extends ConsumerWidget {
  const RmapDesktopApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final themeMode = ref.watch(themeModeProvider);
    final router = ref.watch(appRouterProvider);

    return MaterialApp.router(
      title: 'Rmap Workspace',
      debugShowCheckedModeBanner: false,
      theme: RmapTheme.light(),
      darkTheme: RmapTheme.dark(),
      themeMode: themeMode,
      routerConfig: router,
    );
  }
}
