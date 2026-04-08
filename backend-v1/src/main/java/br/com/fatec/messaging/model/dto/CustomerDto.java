package br.com.fatec.messaging.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CustomerDto {
    private Integer id;
    private String name;
    private String email;
    private String document;
}
