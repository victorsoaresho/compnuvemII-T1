package br.com.fatec.messaging.repository;

import br.com.fatec.messaging.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    Optional<Customer> findByDocument(String document);

    @Modifying
    @Query(value = "INSERT INTO customer (name, email, document) VALUES (:name, :email, :document) ON CONFLICT (document) DO NOTHING",
            nativeQuery = true)
    void upsertIfAbsent(@Param("name") String name, @Param("email") String email, @Param("document") String document);
}
