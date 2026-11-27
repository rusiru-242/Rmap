import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'rmap_tokens.dart';

final themeModeProvider = StateProvider<ThemeMode>((ref) => ThemeMode.system);

abstract final class RmapTheme {
  static ThemeData light() {
    const scheme = ColorScheme.light(
      primary: RmapTokens.accentLight,
      onPrimary: Colors.white,
      surface: RmapTokens.lightSurface,
      onSurface: RmapTokens.lightTextPrimary,
      outline: RmapTokens.lightBorder,
      error: RmapTokens.danger,
    );
    return _build(
      brightness: Brightness.light,
      scheme: scheme,
      scaffoldBg: RmapTokens.lightBg,
      surface: RmapTokens.lightSurface,
      surfaceSubtle: RmapTokens.lightSurfaceSubtle,
      border: RmapTokens.lightBorder,
      textPrimary: RmapTokens.lightTextPrimary,
      textSecondary: RmapTokens.lightTextSecondary,
    );
  }

  static ThemeData dark() {
    const scheme = ColorScheme.dark(
      primary: RmapTokens.accentDark,
      onPrimary: Colors.white,
      surface: RmapTokens.darkSurface,
      onSurface: RmapTokens.darkTextPrimary,
      outline: RmapTokens.darkBorder,
      error: RmapTokens.danger,
    );
    return _build(
      brightness: Brightness.dark,
      scheme: scheme,
      scaffoldBg: RmapTokens.darkBg,
      surface: RmapTokens.darkSurface,
      surfaceSubtle: RmapTokens.darkSurfaceSubtle,
      border: RmapTokens.darkBorder,
      textPrimary: RmapTokens.darkTextPrimary,
      textSecondary: RmapTokens.darkTextSecondary,
    );
  }

  static ThemeData _build({
    required Brightness brightness,
    required ColorScheme scheme,
    required Color scaffoldBg,
    required Color surface,
    required Color surfaceSubtle,
    required Color border,
    required Color textPrimary,
    required Color textSecondary,
  }) {
    final baseText = TextTheme(
      headlineSmall: TextStyle(fontSize: 18, fontWeight: FontWeight.w600, color: textPrimary, letterSpacing: -0.2),
      titleMedium: TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: textPrimary),
      titleSmall: TextStyle(fontSize: 12.5, fontWeight: FontWeight.w600, color: textPrimary),
      bodyMedium: TextStyle(fontSize: 13, fontWeight: FontWeight.w400, color: textPrimary, height: 1.45),
      bodySmall: TextStyle(fontSize: 12, fontWeight: FontWeight.w400, color: textSecondary, height: 1.4),
      labelSmall: TextStyle(fontSize: 11, fontWeight: FontWeight.w500, color: textSecondary, letterSpacing: 0.4),
    );

    return ThemeData(
      useMaterial3: true,
      brightness: brightness,
      colorScheme: scheme,
      scaffoldBackgroundColor: scaffoldBg,
      dividerColor: border,
      textTheme: baseText,
      cardTheme: CardThemeData(
        color: surface,
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(RmapTokens.radiusMd),
          side: BorderSide(color: border),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: surfaceSubtle,
        isDense: true,
        contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 10),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(RmapTokens.radiusMd),
          borderSide: BorderSide(color: border),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(RmapTokens.radiusMd),
          borderSide: BorderSide(color: border),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(RmapTokens.radiusMd),
          borderSide: BorderSide(color: scheme.primary, width: 1.2),
        ),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          textStyle: const TextStyle(fontSize: 12.5, fontWeight: FontWeight.w500),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(RmapTokens.radiusMd),
          ),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          textStyle: const TextStyle(fontSize: 12.5, fontWeight: FontWeight.w500),
          side: BorderSide(color: border),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(RmapTokens.radiusMd),
          ),
        ),
      ),
    );
  }
}
