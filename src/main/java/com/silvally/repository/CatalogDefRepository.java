package com.silvally.repository;

import com.silvally.entity.CatalogDef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CatalogDefRepository extends JpaRepository<CatalogDef, CatalogDef.Id> {
    @Query("""
            select c from CatalogDef c
            where c.id.rulesVersion = :rulesVersion and c.id.kind = :kind
            order by c.id.code asc
            """)
    List<CatalogDef> findByKind(@Param("rulesVersion") String rulesVersion, @Param("kind") String kind);
}
