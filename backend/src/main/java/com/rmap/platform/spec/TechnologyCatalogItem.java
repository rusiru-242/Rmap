package com.rmap.platform.spec;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "technology_catalog")
public class TechnologyCatalogItem {
    @Id
    private String id;
    @Indexed
    private String category;
    @Indexed(unique = true)
    private String name;
    private String summary;
    private List<String> strengths;
    private List<String> tradeoffs;
    private List<String> compatibleWith;
    private List<String> incompatibleWith;
    private boolean customEntry;

    public TechnologyCatalogItem() {}

    public TechnologyCatalogItem(
            String category,
            String name,
            String summary,
            List<String> strengths,
            List<String> tradeoffs,
            List<String> compatibleWith,
            List<String> incompatibleWith,
            boolean customEntry
    ) {
        this.category = category;
        this.name = name;
        this.summary = summary;
        this.strengths = strengths;
        this.tradeoffs = tradeoffs;
        this.compatibleWith = compatibleWith;
        this.incompatibleWith = incompatibleWith;
        this.customEntry = customEntry;
    }

    public String getId() { return id; }
    public String getCategory() { return category; }
    public String getName() { return name; }
    public String getSummary() { return summary; }
    public List<String> getStrengths() { return strengths; }
    public List<String> getTradeoffs() { return tradeoffs; }
    public List<String> getCompatibleWith() { return compatibleWith; }
    public List<String> getIncompatibleWith() { return incompatibleWith; }
    public boolean isCustomEntry() { return customEntry; }
}
