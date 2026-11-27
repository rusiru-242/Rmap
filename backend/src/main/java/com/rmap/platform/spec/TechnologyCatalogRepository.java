package com.rmap.platform.spec;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TechnologyCatalogRepository extends MongoRepository<TechnologyCatalogItem, String> {
    List<TechnologyCatalogItem> findByCategoryIgnoreCase(String category);
    boolean existsByNameIgnoreCase(String name);
}
