package com.dogus.vacation.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Departman ({@code departments}). Departman silinmez.
 */
@Entity
@Table(name = "departments")
public class Department extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** JPA için; uygulama kodu kullanmaz. */
    protected Department() {
    }

    public Department(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
