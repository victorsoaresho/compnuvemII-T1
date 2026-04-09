package br.com.fatec.messaging.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PaymentDto {
    private String method;
    private String status;

    @JsonProperty("transaction_id")
    private String transactionId;
}
