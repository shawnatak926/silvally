package com.silvally.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "catalog_def")
public class CatalogDef {
    @Embeddable
    public record Id(String rulesVersion, String kind, String code) implements Serializable {
    }

    @EmbeddedId
    private Id id;

    @Column(nullable = false)
    private String title;

    @Lob
    private String configJson;

    protected CatalogDef() {
    }

    public Id getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getConfigJson() {
        return configJson;
    }
}
