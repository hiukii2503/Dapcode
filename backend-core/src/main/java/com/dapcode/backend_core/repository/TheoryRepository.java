package com.dapcode.backend_core.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import com.dapcode.backend_core.entity.Theories;

public interface TheoryRepository extends JpaRepository<Theories, UUID> {
  Optional<Theories> findBySlugAndIsPublishedTrue(String slug);

  boolean existsBySlug(String slug);

  @Query("""
          SELECT t FROM Theories t
          WHERE t.isPublished = true
            AND (cast(:category as String) IS NULL OR LOWER(t.category) = LOWER(cast(:category as String)))
          ORDER BY t.category ASC, t.orderIndex ASC
      """)
  List<Theories> findPublishedTheories(@Param("category") String category);

  @Query(value = """
          SELECT t FROM Theories t
          WHERE t.isPublished = true
            AND (cast(:category as String) IS NULL OR LOWER(t.category) = LOWER(cast(:category as String)))
            AND (cast(:search as String) IS NULL
                 OR LOWER(t.title) LIKE LOWER(CONCAT('%', cast(:search as String), '%'))
                 OR LOWER(COALESCE(t.summary, '')) LIKE LOWER(CONCAT('%', cast(:search as String), '%'))
                 OR LOWER(t.category) LIKE LOWER(CONCAT('%', cast(:search as String), '%')))
          ORDER BY t.category ASC, t.orderIndex ASC
      """, countQuery = """
          SELECT COUNT(t) FROM Theories t
          WHERE t.isPublished = true
            AND (cast(:category as String) IS NULL OR LOWER(t.category) = LOWER(cast(:category as String)))
            AND (cast(:search as String) IS NULL
                 OR LOWER(t.title) LIKE LOWER(CONCAT('%', cast(:search as String), '%'))
                 OR LOWER(COALESCE(t.summary, '')) LIKE LOWER(CONCAT('%', cast(:search as String), '%'))
                 OR LOWER(t.category) LIKE LOWER(CONCAT('%', cast(:search as String), '%')))
      """)
  Page<Theories> findPublishedTheoriesPaged(
          @Param("category") String category,
          @Param("search") String search,
          Pageable pageable);

  @Query("SELECT DISTINCT t.category FROM Theories t WHERE t.isPublished = true ORDER BY t.category ASC")
  List<String> findDistinctCategories();

  Optional<Theories> findBySlug(String slug);

  @Query("""
          SELECT t FROM Theories t
          WHERE (cast(:category as String) IS NULL OR LOWER(t.category) = LOWER(cast(:category as String)))
          ORDER BY t.category ASC, t.orderIndex ASC
      """)
  List<Theories> findAllTheoriesForAdmin(@Param("category") String category);

}
