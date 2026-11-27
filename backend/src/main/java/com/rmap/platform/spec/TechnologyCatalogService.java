package com.rmap.platform.spec;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class TechnologyCatalogService implements CommandLineRunner {
    private final TechnologyCatalogRepository repository;

    public TechnologyCatalogService(TechnologyCatalogRepository repository) {
        this.repository = repository;
    }

    public record ValidationReport(
            boolean valid,
            List<String> violations,
            List<String> warnings
    ) {}

    @Override
    public void run(String... args) {
        if (repository.count() > 0) return;
        seedDefaults();
    }

    public List<TechnologyCatalogItem> list(String category) {
        if (category != null && !category.isBlank()) {
            return repository.findByCategoryIgnoreCase(category.trim());
        }
        return repository.findAll();
    }

    public TechnologyCatalogItem addCustom(TechnologyCatalogItem item) {
        return repository.save(new TechnologyCatalogItem(
                item.getCategory(),
                item.getName(),
                item.getSummary(),
                item.getStrengths() == null ? List.of() : item.getStrengths(),
                item.getTradeoffs() == null ? List.of() : item.getTradeoffs(),
                item.getCompatibleWith() == null ? List.of() : item.getCompatibleWith(),
                item.getIncompatibleWith() == null ? List.of() : item.getIncompatibleWith(),
                true
        ));
    }

    /**
     * Deterministic constraint & compatibility validation.
     * Mandatory and forbidden project constraints ALWAYS override AI recommendations.
     */
    public ValidationReport validateSelection(
            List<String> selectedTechnologies,
            List<String> mandatoryConstraints,
            List<String> forbiddenTechnologies
    ) {
        List<String> normalized = selectedTechnologies == null ? List.of()
                : selectedTechnologies.stream().map(s -> s.trim().toLowerCase(Locale.ROOT)).toList();

        List<String> violations = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (forbiddenTechnologies != null) {
            for (String forbidden : forbiddenTechnologies) {
                String f = forbidden.trim().toLowerCase(Locale.ROOT);
                if (!f.isBlank() && normalized.stream().anyMatch(s -> s.contains(f))) {
                    violations.add("Forbidden technology selected: " + forbidden + " (violates mandatory constraint).");
                }
            }
        }

        if (mandatoryConstraints != null) {
            for (String mandatory : mandatoryConstraints) {
                String m = mandatory.trim().toLowerCase(Locale.ROOT);
                if (m.startsWith("must use ") || m.startsWith("required:")) {
                    String requiredTech = m.replaceFirst("^(must use |required:)\\s*", "").trim();
                    if (!requiredTech.isBlank() && normalized.stream().noneMatch(s -> s.contains(requiredTech))) {
                        violations.add("Mandatory technology missing from selection: " + mandatory);
                    }
                }
            }
        }

        if (normalized.contains("swift") && normalized.contains("kotlin")) {
            warnings.add("Dual native mobile stacks (Swift + Kotlin) increase effort; consider Flutter or React Native unless native parity is required.");
        }

        return new ValidationReport(violations.isEmpty(), violations, warnings);
    }

    private void seedDefaults() {
        List<TechnologyCatalogItem> seed = List.of(
                item("Frontend", "React", "Component-based UI library with strong ecosystem"),
                item("Frontend", "Next.js", "Full-stack React framework with SSR/SSG"),
                item("Frontend", "Angular", "Opinionated TypeScript enterprise web framework"),
                item("Frontend", "Vue", "Progressive frontend framework"),
                item("Frontend", "Svelte", "Compiled reactive frontend framework"),
                item("Frontend", "HTML/CSS/JS", "Zero-framework standard web foundation"),
                item("Backend", "Spring Boot", "Java/Kotlin enterprise modular monolith & microservice framework"),
                item("Backend", "ASP.NET Core", "Cross-platform C# high-performance web/API framework"),
                item("Backend", "Node.js", "JavaScript/TypeScript event-driven server runtime"),
                item("Backend", "Express", "Minimal Node.js HTTP framework"),
                item("Backend", "NestJS", "Structured TypeScript backend framework"),
                item("Backend", "Django", "Batteries-included Python web framework"),
                item("Backend", "FastAPI", "High-performance typed Python API framework"),
                item("Backend", "Laravel", "Full-featured PHP web application framework"),
                item("Mobile", "Flutter", "Cross-platform Dart UI toolkit for mobile and desktop"),
                item("Mobile", "React Native", "Cross-platform mobile framework using React"),
                item("Mobile", "Kotlin", "Native Android development language"),
                item("Mobile", "Swift", "Native iOS/macOS development language"),
                item("Desktop", "Electron", "Cross-platform desktop apps with web technologies"),
                item("Desktop", "Tauri", "Lightweight Rust + web desktop framework"),
                item("Desktop", ".NET", "Windows-first desktop (WPF/WinUI/MAUI)"),
                item("Desktop", "JavaFX", "JVM desktop UI toolkit"),
                item("Database", "MongoDB", "Document database with flexible schema and replica sets"),
                item("Database", "PostgreSQL", "ACID relational database with JSONB and extensions"),
                item("Database", "MySQL", "Widely deployed relational database"),
                item("Database", "SQLite", "Embedded zero-config relational database"),
                item("Database", "Firebase", "Managed real-time NoSQL & auth platform"),
                item("Database", "Supabase", "Managed PostgreSQL + Auth + Storage"),
                item("Database", "Redis", "In-memory cache, OTP store, rate limiter, and queue"),
                item("AI and Machine Learning", "Python", "Primary ecosystem for ML and data science"),
                item("AI and Machine Learning", "pandas", "Tabular data analysis and manipulation"),
                item("AI and Machine Learning", "NumPy", "Numerical array computing"),
                item("AI and Machine Learning", "scikit-learn", "Classical machine learning & validation"),
                item("AI and Machine Learning", "TensorFlow", "Production deep learning framework"),
                item("AI and Machine Learning", "Keras", "High-level neural network API"),
                item("AI and Machine Learning", "PyTorch", "Research & production deep learning framework"),
                item("Agentic AI", "LangGraph", "Stateful multi-agent graph orchestration"),
                item("Agentic AI", "Google ADK", "Google Agent Development Kit for Gemini agents"),
                item("Agentic AI", "Custom Orchestration", "Explicit deterministic state-machine agent orchestration"),
                item("Authentication", "JWT + Refresh Rotation", "Stateless access tokens with revocable refresh families"),
                item("Cloud Infrastructure", "Docker Compose", "Reproducible multi-container local & small-scale orchestration"),
                item("Hosting", "Render / Fly.io / Cloud Run", "Managed container hosting options"),
                item("Testing", "JUnit 5 + Vitest + Flutter Test", "Automated multi-layer test verification"),
                item("Security", "OWASP ASVS + RBAC", "Role-based access control and input validation baseline"),
                item("DevOps", "GitHub Actions", "Automated CI/CD pipelines")
        );
        repository.saveAll(seed);
    }

    private TechnologyCatalogItem item(String cat, String name, String summary) {
        return new TechnologyCatalogItem(cat, name, summary, List.of(), List.of(), List.of(), List.of(), false);
    }
}
