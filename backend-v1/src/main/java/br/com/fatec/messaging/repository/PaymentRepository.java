package br.com.fatec.messaging.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.fatec.messaging.model.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

}
