package com.example.javalab1.persistence.repository;

import com.example.javalab1.persistence.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {
    Optional<CategoryEntity> findByPublicId(UUID publicId);
}
