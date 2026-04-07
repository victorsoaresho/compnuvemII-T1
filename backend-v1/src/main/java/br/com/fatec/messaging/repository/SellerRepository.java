package br.com.fatec.messaging.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.fatec.messaging.model.Seller;

@Repository
public interface SellerRepository extends JpaRepository<Seller, Integer> {

}
