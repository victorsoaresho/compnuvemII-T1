package br.com.fatec.messaging.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SellerDto {
    private Integer id;
    private String name;
    private String city;
    private String state;
}
