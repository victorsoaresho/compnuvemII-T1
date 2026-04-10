package br.com.fatec.messaging.repository;

import br.com.fatec.messaging.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {

    @Modifying
    @Query(value = "INSERT INTO category (id, name) VALUES (:id, :name) ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name",
            nativeQuery = true)
    void upsert(@Param("id") String id, @Param("name") String name);
}
