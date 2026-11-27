import 'package:flutter/material.dart';

enum ProjectContextMode {
  general,
  academic,
  team,
  ml,
  client,
}

class NavItemSpec {
  final String id;
  final String label;
  final String route;
  final IconData icon;
  final String group;
  final int phase;
  final Set<ProjectContextMode>? onlyForModes;

  const NavItemSpec({
    required this.id,
    required this.label,
    required this.route,
    required this.icon,
    required this.group,
    this.phase = 1,
    this.onlyForModes,
  });

  bool isVisibleFor(ProjectContextMode mode) {
    if (onlyForModes == null || onlyForModes!.isEmpty) return true;
    return onlyForModes!.contains(mode);
  }
}

class NavGroupSpec {
  final String title;
  final List<NavItemSpec> items;

  const NavGroupSpec({required this.title, required this.items});
}

/// Data-driven navigation registry covering all 8 Rmap workspace groups.
abstract final class RmapNavRegistry {
  static const List<NavGroupSpec> groups = [
    NavGroupSpec(
      title: 'WORKSPACE',
      items: [
        NavItemSpec(id: 'dashboard', label: 'Dashboard', route: '/dashboard', icon: Icons.space_dashboard_outlined, group: 'WORKSPACE'),
        NavItemSpec(id: 'projects', label: 'My Projects', route: '/projects', icon: Icons.folder_outlined, group: 'WORKSPACE', phase: 2),
        NavItemSpec(id: 'create-project', label: 'Create Project', route: '/projects/create', icon: Icons.add_box_outlined, group: 'WORKSPACE', phase: 2),
        NavItemSpec(id: 'import-project', label: 'Import Project', route: '/workspace/local', icon: Icons.drive_folder_upload_outlined, group: 'WORKSPACE'),
      ],
    ),
    NavGroupSpec(
      title: 'DISCOVERY',
      items: [
        NavItemSpec(id: 'requirements', label: 'Project Requirements', route: '/discovery/requirements', icon: Icons.checklist_outlined, group: 'DISCOVERY', phase: 2),
        NavItemSpec(id: 'documents', label: 'Documents', route: '/discovery/documents', icon: Icons.description_outlined, group: 'DISCOVERY', phase: 2),
        NavItemSpec(id: 'ai-research', label: 'AI Research', route: '/discovery/research', icon: Icons.travel_explore_outlined, group: 'DISCOVERY', phase: 2),
        NavItemSpec(id: 'req-review', label: 'Requirement Review', route: '/discovery/review', icon: Icons.fact_check_outlined, group: 'DISCOVERY', phase: 2),
      ],
    ),
    NavGroupSpec(
      title: 'DESIGN',
      items: [
        NavItemSpec(id: 'architecture', label: 'Architecture', route: '/design/architecture', icon: Icons.account_tree_outlined, group: 'DESIGN', phase: 2),
        NavItemSpec(id: 'database', label: 'Database Design', route: '/design/database', icon: Icons.storage_outlined, group: 'DESIGN', phase: 2),
        NavItemSpec(id: 'api', label: 'API Design', route: '/design/api', icon: Icons.api_outlined, group: 'DESIGN', phase: 2),
        NavItemSpec(id: 'uiux', label: 'UI/UX Studio', route: '/design/uiux', icon: Icons.design_services_outlined, group: 'DESIGN', phase: 3),
        NavItemSpec(id: 'ref-analyzer', label: 'Reference Analyzer', route: '/design/references', icon: Icons.image_search_outlined, group: 'DESIGN', phase: 3),
        NavItemSpec(id: 'design-import', label: 'Design Import', route: '/design/import', icon: Icons.archive_outlined, group: 'DESIGN', phase: 3),
      ],
    ),
    NavGroupSpec(
      title: 'ENGINEERING',
      items: [
        NavItemSpec(id: 'env-setup', label: 'Environment Setup', route: '/engineering/env', icon: Icons.terminal_outlined, group: 'ENGINEERING', phase: 3),
        NavItemSpec(id: 'phase-planner', label: 'Phase Planner', route: '/engineering/phases', icon: Icons.view_timeline_outlined, group: 'ENGINEERING', phase: 3),
        NavItemSpec(id: 'master-prompts', label: 'Master Prompts', route: '/engineering/prompts', icon: Icons.auto_awesome_outlined, group: 'ENGINEERING', phase: 3),
        NavItemSpec(id: 'agent-manager', label: 'Agent Manager', route: '/engineering/agents', icon: Icons.hub_outlined, group: 'ENGINEERING', phase: 3),
        NavItemSpec(id: 'repo-explorer', label: 'Repository Explorer', route: '/workspace/local', icon: Icons.code_outlined, group: 'ENGINEERING'),
        NavItemSpec(id: 'tasks', label: 'Tasks', route: '/engineering/tasks', icon: Icons.task_alt_outlined, group: 'ENGINEERING', phase: 3),
        NavItemSpec(id: 'error-fix', label: 'Error Fix Studio', route: '/engineering/errors', icon: Icons.build_circle_outlined, group: 'ENGINEERING', phase: 3),
      ],
    ),
    NavGroupSpec(
      title: 'QUALITY',
      items: [
        NavItemSpec(id: 'testing', label: 'Testing', route: '/quality/testing', icon: Icons.science_outlined, group: 'QUALITY', phase: 3),
        NavItemSpec(id: 'security', label: 'Security', route: '/quality/security', icon: Icons.shield_outlined, group: 'QUALITY', phase: 3),
        NavItemSpec(id: 'traceability', label: 'Traceability', route: '/quality/traceability', icon: Icons.linear_scale_outlined, group: 'QUALITY', phase: 3),
        NavItemSpec(id: 'documentation', label: 'Documentation', route: '/quality/docs', icon: Icons.menu_book_outlined, group: 'QUALITY', phase: 3),
      ],
    ),
    NavGroupSpec(
      title: 'DELIVERY',
      items: [
        NavItemSpec(id: 'deployment', label: 'Deployment', route: '/delivery/deployment', icon: Icons.rocket_launch_outlined, group: 'DELIVERY', phase: 3),
        NavItemSpec(id: 'client-delivery', label: 'Client Delivery', route: '/delivery/client', icon: Icons.handshake_outlined, group: 'DELIVERY', phase: 3),
        NavItemSpec(id: 'submission', label: 'Submission', route: '/delivery/submission', icon: Icons.inventory_2_outlined, group: 'DELIVERY', phase: 3),
        NavItemSpec(id: 'maintenance', label: 'Maintenance', route: '/delivery/maintenance', icon: Icons.handyman_outlined, group: 'DELIVERY', phase: 3),
      ],
    ),
    NavGroupSpec(
      title: 'PROJECT-SPECIFIC',
      items: [
        NavItemSpec(id: 'academic', label: 'Academic Studio', route: '/specific/academic', icon: Icons.school_outlined, group: 'PROJECT-SPECIFIC', phase: 2, onlyForModes: {ProjectContextMode.academic}),
        NavItemSpec(id: 'team', label: 'Team and Ownership', route: '/specific/team', icon: Icons.groups_outlined, group: 'PROJECT-SPECIFIC', phase: 3, onlyForModes: {ProjectContextMode.team, ProjectContextMode.academic, ProjectContextMode.client}),
        NavItemSpec(id: 'ml', label: 'ML Experiments', route: '/specific/ml', icon: Icons.insights_outlined, group: 'PROJECT-SPECIFIC', phase: 3, onlyForModes: {ProjectContextMode.ml}),
        NavItemSpec(id: 'milestones', label: 'Milestones', route: '/specific/milestones', icon: Icons.flag_outlined, group: 'PROJECT-SPECIFIC', phase: 3),
        NavItemSpec(id: 'viva', label: 'Viva Preparation', route: '/specific/viva', icon: Icons.record_voice_over_outlined, group: 'PROJECT-SPECIFIC', phase: 3, onlyForModes: {ProjectContextMode.academic}),
        NavItemSpec(id: 'ai-usage-log', label: 'AI Usage Log', route: '/specific/ai-usage', icon: Icons.receipt_long_outlined, group: 'PROJECT-SPECIFIC', phase: 2, onlyForModes: {ProjectContextMode.academic, ProjectContextMode.team, ProjectContextMode.client}),
      ],
    ),
    NavGroupSpec(
      title: 'ACCOUNT',
      items: [
        NavItemSpec(id: 'profile', label: 'Profile', route: '/account/profile', icon: Icons.person_outline, group: 'ACCOUNT'),
        NavItemSpec(id: 'subscription', label: 'Subscription', route: '/account/subscription', icon: Icons.credit_card_outlined, group: 'ACCOUNT'),
        NavItemSpec(id: 'settings', label: 'Settings', route: '/account/settings', icon: Icons.settings_outlined, group: 'ACCOUNT'),
      ],
    ),
  ];

  static List<NavItemSpec> visibleItems(ProjectContextMode mode) {
    return groups
        .expand((g) => g.items)
        .where((item) => item.isVisibleFor(mode))
        .toList();
  }
}
