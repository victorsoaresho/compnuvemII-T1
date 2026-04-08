package br.com.fatec.messaging.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ShipmentDto {
    private String carrier;
    private String service;
    private String status;

    @JsonProperty("tracking_code")
    private String trackingCode;
}
