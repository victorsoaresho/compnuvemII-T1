package br.com.fatec.messaging.repository;

import br.com.fatec.messaging.model.SubCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubCategoryRepository extends JpaRepository<SubCategory, String> {

    @Modifying
    @Query(value = "INSERT INTO sub_category (id, name, category_id) VALUES (:id, :name, :categoryId) ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, category_id = EXCLUDED.category_id",
            nativeQuery = true)
    void upsert(@Param("id") String id, @Param("name") String name, @Param("categoryId") String categoryId);
}
