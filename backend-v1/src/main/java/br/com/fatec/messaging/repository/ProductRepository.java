package br.com.fatec.messaging.repository;

import br.com.fatec.messaging.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    Optional<Product> findByProductId(Integer productId);

    @Modifying
    @Query(value = "INSERT INTO product (product_id, product_name, sub_category_id, unit_price) VALUES (:productId, :productName, :subCategoryId, :unitPrice) ON CONFLICT (product_id) DO NOTHING",
            nativeQuery = true)
    void upsertIfAbsent(@Param("productId") Integer productId,
                        @Param("productName") String productName,
                        @Param("subCategoryId") String subCategoryId,
                        @Param("unitPrice") BigDecimal unitPrice);
}
