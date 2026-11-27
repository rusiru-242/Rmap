import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../features/auth/auth_controller.dart';
import '../../features/auth/auth_screen.dart';
import '../../features/dashboard/dashboard_screen.dart';
import '../../features/settings/account_screens.dart';
import '../../features/shell/rmap_shell.dart';
import '../../features/workspace/local_workspace_screen.dart';
import '../navigation/nav_registry.dart';

final appRouterProvider = Provider<GoRouter>((ref) {
  final isAuthenticated = ref.watch(authControllerProvider.select((s) => s.isAuthenticated));

  return GoRouter(
    initialLocation: '/dashboard',
    redirect: (context, state) {
      final onAuth = state.matchedLocation == '/auth';
      if (!isAuthenticated && !onAuth) return '/auth';
      if (isAuthenticated && onAuth) return '/dashboard';
      return null;
    },
    routes: [
      GoRoute(
        path: '/auth',
        builder: (context, state) => const AuthScreen(),
      ),
      ShellRoute(
        builder: (context, state, child) => RmapShell(
          currentLocation: state.matchedLocation,
          child: child,
        ),
        routes: [
          GoRoute(path: '/dashboard', builder: (_, _) => const DashboardScreen()),
          GoRoute(path: '/workspace/local', builder: (_, _) => const LocalWorkspaceScreen()),
          GoRoute(path: '/account/profile', builder: (_, _) => const ProfileScreen()),
          GoRoute(path: '/account/subscription', builder: (_, _) => const SubscriptionScreen()),
          GoRoute(path: '/account/settings', builder: (_, _) => const SettingsScreen()),
          for (final item in RmapNavRegistry.groups.expand((g) => g.items))
            if (item.phase > 1)
              GoRoute(
                path: item.route,
                builder: (_, _) => RoadmapModulePlaceholder(title: item.label, phase: item.phase),
              ),
        ],
      ),
    ],
  );
});
