package br.com.fatec.messaging.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "category")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Category {

    @Id
    private String id;

    @Column(name = "name", nullable = false)
    private String name;
}
