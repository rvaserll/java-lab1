package com.example.javalab1.persistence.repository;

import com.example.javalab1.persistence.entity.ProductEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    @EntityGraph(attributePaths = "category")
    Optional<ProductEntity> findByPublicId(UUID publicId);

    boolean existsByCategoryId(Long categoryId);

    // UUID.compareTo compares both 64-bit halves as signed numbers. PostgreSQL's
    // default UUID ordering is unsigned; preserve the existing API order explicitly.
    @EntityGraph(attributePaths = "category")
    @Query("""
            select p from ProductEntity p order by
            case when substring(cast(p.publicId as string), 1, 1) >= '8' then 0 else 1 end,
            substring(cast(p.publicId as string), 1, 18),
            case when substring(cast(p.publicId as string), 20, 1) >= '8' then 0 else 1 end,
            substring(cast(p.publicId as string), 20)
            """)
    List<ProductEntity> findInApiOrder(Pageable pageable);
}
