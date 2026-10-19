
package com.efe.veterinaryclinic.catalog;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(
    name = "procedure_catalog",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"code", "species"}
    )
)
@Getter
public class ProcedureCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String species;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false)
    private boolean active = true;

    protected ProcedureCatalog() {
    }

    public ProcedureCatalog(
            String code,
            String name,
            String species,
            String category) {

        this.code = code;
        this.name = name;
        this.species = species;
        this.category = category;
        this.active = true;
    }
}
