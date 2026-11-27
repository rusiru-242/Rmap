package com.rmap.platform.billing;

import java.util.List;

public record SubscriptionPlan(
        String code,
        String name,
        String billingInterval,
        long priceMinor,
        String currency,
        List<String> features
) {
    public static final List<SubscriptionPlan> CATALOG = List.of(
            new SubscriptionPlan(
                    "PRO_MONTHLY",
                    "Rmap Pro Monthly",
                    "MONTH",
                    2900,
                    "USD",
                    List.of(
                            "Unlimited projects and ProjectSpec versions",
                            "All 4 internal Gemini AI engineering agents",
                            "Local repository intelligence & verification",
                            "Markdown, PDF, and DOCX exports"
                    )
            ),
            new SubscriptionPlan(
                    "PRO_ANNUAL",
                    "Rmap Pro Annual",
                    "YEAR",
                    29000,
                    "USD",
                    List.of(
                            "Everything in Rmap Pro Monthly",
                            "2 months free with annual billing",
                            "Priority support & early feature access"
                    )
            ),
            new SubscriptionPlan(
                    "TEAM_MONTHLY",
                    "Rmap Team",
                    "MONTH",
                    7900,
                    "USD",
                    List.of(
                            "Up to 5 seats with role & task ownership",
                            "Shared ProjectSpec and traceability matrix",
                            "Team contribution & verification tracking"
                    )
            )
    );
}
