package br.com.fatec.messaging.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.fatec.messaging.model.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {

}
