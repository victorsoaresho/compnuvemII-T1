package br.com.fatec.messaging.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
public class OrderPayloadDto {
    private String uuid;

    @JsonProperty("created_at")
    private String createdAt;

    private String channel;
    private BigDecimal total;
    private String status;
    private CustomerDto customer;
    private SellerDto seller;
    private List<ItemDto> items;
    private ShipmentDto shipment;
    private PaymentDto payment;
    private MetadataDto metadata;
}
