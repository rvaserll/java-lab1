package com.example.javalab1.persistence.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "categories")
public class CategoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "categories_generator")
    @SequenceGenerator(name = "categories_generator", sequenceName = "categories_id_seq", allocationSize = 1)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    protected CategoryEntity() {}
    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }

    @Column(nullable = false, columnDefinition = "text")
    private String name;

    public CategoryEntity(UUID publicId, String name) {
        this.publicId = publicId;
        this.name = name;
    }
    public String getName() { return name; }
    public void rename(String name) { this.name = name; }
}
