import 'package:flutter/material.dart';

/// Centralized Rmap design tokens inspired by Linear & VS Code.
/// Neutral palette, single restrained accent, compact typography, subtle radii.
abstract final class RmapTokens {
  // Spacing scale (4px grid)
  static const double space1 = 4;
  static const double space2 = 8;
  static const double space3 = 12;
  static const double space4 = 16;
  static const double space5 = 20;
  static const double space6 = 24;
  static const double space8 = 32;

  // Radii (subtle, professional — no excessive rounded containers)
  static const double radiusSm = 4;
  static const double radiusMd = 6;
  static const double radiusLg = 8;

  // Layout dimensions
  static const double sidebarExpandedWidth = 244;
  static const double sidebarCollapsedWidth = 56;
  static const double topBarHeight = 44;
  static const double statusBarHeight = 26;
  static const double aiPanelMinWidth = 260;
  static const double aiPanelMaxWidth = 520;
  static const double aiPanelDefaultWidth = 320;

  // Restrained single accent color (Indigo)
  static const Color accentLight = Color(0xFF4F46E5);
  static const Color accentDark = Color(0xFF6366F1);

  // Semantic colors
  static const Color success = Color(0xFF16A34A);
  static const Color warning = Color(0xFFD97706);
  static const Color danger = Color(0xFFDC2626);

  // Neutral palette — Light
  static const Color lightBg = Color(0xFFFAFAFB);
  static const Color lightSurface = Color(0xFFFFFFFF);
  static const Color lightSurfaceSubtle = Color(0xFFF4F4F6);
  static const Color lightBorder = Color(0xFFE4E4E7);
  static const Color lightTextPrimary = Color(0xFF18181B);
  static const Color lightTextSecondary = Color(0xFF52525B);
  static const Color lightTextMuted = Color(0xFF71717A);

  // Neutral palette — Dark
  static const Color darkBg = Color(0xFF0F1012);
  static const Color darkSurface = Color(0xFF16171B);
  static const Color darkSurfaceSubtle = Color(0xFF1E2026);
  static const Color darkBorder = Color(0xFF27282E);
  static const Color darkTextPrimary = Color(0xFFF4F4F5);
  static const Color darkTextSecondary = Color(0xFFA1A1AA);
  static const Color darkTextMuted = Color(0xFF71717A);
}
