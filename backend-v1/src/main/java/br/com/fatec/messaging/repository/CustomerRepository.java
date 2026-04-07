package br.com.fatec.messaging.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.fatec.messaging.model.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

}
