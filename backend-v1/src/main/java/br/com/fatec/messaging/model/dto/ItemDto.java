package br.com.fatec.messaging.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class ItemDto {
    private Integer id;

    @JsonProperty("product_id")
    private Integer productId;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("unit_price")
    private BigDecimal unitPrice;

    private Integer quantity;
    private CategoryDto category;
    private BigDecimal total;
}
