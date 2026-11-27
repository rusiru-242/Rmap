import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../features/academic/academic_and_ml_screens.dart';
import '../../features/agents/agent_manager_screen.dart';
import '../../features/auth/auth_controller.dart';
import '../../features/auth/auth_screen.dart';
import '../../features/dashboard/dashboard_screen.dart';
import '../../features/delivery/delivery_screens.dart';
import '../../features/design/design_screens.dart';
import '../../features/discovery/discovery_screens.dart';
import '../../features/execution/execution_screens.dart';
import '../../features/projects/projects_screens.dart';
import '../../features/settings/account_screens.dart';
import '../../features/shell/rmap_shell.dart';
import '../../features/workspace/local_workspace_screen.dart';

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
      GoRoute(path: '/auth', builder: (_, _) => const AuthScreen()),
      ShellRoute(
        builder: (context, state, child) => RmapShell(
          currentLocation: state.matchedLocation,
          child: child,
        ),
        routes: [
          GoRoute(path: '/dashboard', builder: (_, _) => const DashboardScreen()),
          GoRoute(path: '/projects', builder: (_, _) => const MyProjectsScreen()),
          GoRoute(path: '/projects/create', builder: (_, _) => const CreateProjectWizardScreen()),
          GoRoute(path: '/workspace/local', builder: (_, _) => const LocalWorkspaceScreen()),
          GoRoute(path: '/discovery/requirements', builder: (_, _) => const RequirementsWorkspaceScreen()),
          GoRoute(path: '/discovery/review', builder: (_, _) => const RequirementsWorkspaceScreen()),
          GoRoute(path: '/discovery/documents', builder: (_, _) => const DocumentsScreen()),
          GoRoute(path: '/discovery/research', builder: (_, _) => const AgentManagerScreen()),
          GoRoute(path: '/design/architecture', builder: (_, _) => const ArchitectureWorkspaceScreen()),
          GoRoute(path: '/design/database', builder: (_, _) => const ArchitectureWorkspaceScreen()),
          GoRoute(path: '/design/api', builder: (_, _) => const ArchitectureWorkspaceScreen()),
          GoRoute(path: '/design/uiux', builder: (_, _) => const UiUxDesignStudioScreen()),
          GoRoute(path: '/design/references', builder: (_, _) => const UiUxDesignStudioScreen()),
          GoRoute(path: '/design/import', builder: (_, _) => const UiUxDesignStudioScreen()),
          GoRoute(path: '/engineering/env', builder: (_, _) => const EnvironmentSetupScreen()),
          GoRoute(path: '/engineering/phases', builder: (_, _) => const PhasePlannerScreen()),
          GoRoute(path: '/engineering/prompts', builder: (_, _) => const MasterPromptsScreen()),
          GoRoute(path: '/engineering/agents', builder: (_, _) => const AgentManagerScreen()),
          GoRoute(path: '/engineering/tasks', builder: (_, _) => const TasksAndKanbanScreen()),
          GoRoute(path: '/engineering/errors', builder: (_, _) => const ErrorFixStudioScreen()),
          GoRoute(path: '/quality/testing', builder: (_, _) => const TestingSecurityAndDeliveryScreen(modeTitle: 'Testing Workspace')),
          GoRoute(path: '/quality/security', builder: (_, _) => const TestingSecurityAndDeliveryScreen(modeTitle: 'Security Engineering Workspace')),
          GoRoute(path: '/quality/traceability', builder: (_, _) => const TraceabilityAndVerificationScreen()),
          GoRoute(path: '/quality/docs', builder: (_, _) => const DocumentationScreen()),
          GoRoute(path: '/delivery/deployment', builder: (_, _) => const DeploymentCenterScreen()),
          GoRoute(path: '/delivery/client', builder: (_, _) => const TestingSecurityAndDeliveryScreen(modeTitle: 'Client Delivery & Handover')),
          GoRoute(path: '/delivery/submission', builder: (_, _) => const TestingSecurityAndDeliveryScreen(modeTitle: 'Academic Submission & Delivery')),
          GoRoute(path: '/delivery/maintenance', builder: (_, _) => const TestingSecurityAndDeliveryScreen(modeTitle: 'Maintenance & Change Management')),
          GoRoute(path: '/specific/academic', builder: (_, _) => const AcademicStudioScreen()),
          GoRoute(path: '/specific/team', builder: (_, _) => const TasksAndKanbanScreen()),
          GoRoute(path: '/specific/ml', builder: (_, _) => const DatasetWorkspaceScreen()),
          GoRoute(path: '/specific/milestones', builder: (_, _) => const PhasePlannerScreen()),
          GoRoute(path: '/specific/viva', builder: (_, _) => const TestingSecurityAndDeliveryScreen(modeTitle: 'Viva Preparation Studio')),
          GoRoute(path: '/specific/ai-usage', builder: (_, _) => const MasterPromptsScreen()),
          GoRoute(path: '/account/profile', builder: (_, _) => const ProfileScreen()),
          GoRoute(path: '/account/subscription', builder: (_, _) => const SubscriptionScreen()),
          GoRoute(path: '/account/settings', builder: (_, _) => const SettingsScreen()),
        ],
      ),
    ],
  );
});
