package br.com.fatec.messaging.model.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderPayloadDtoDeserializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private static final String FULL_JSON = """
            {
              "uuid": "ORD-1",
              "created_at": "2025-10-01T10:15:00Z",
              "channel": "web",
              "total": 199.80,
              "status": "paid",
              "customer": {
                "id": 1,
                "name": "Maria",
                "email": "maria@test.com",
                "document": "12345678900"
              },
              "seller": {
                "id": 5,
                "name": "Loja X",
                "city": "SP",
                "state": "SP"
              },
              "items": [
                {
                  "id": 1,
                  "product_id": 101,
                  "product_name": "Produto A",
                  "unit_price": 99.90,
                  "quantity": 2,
                  "category": {
                    "id": "C1",
                    "name": "Electronics",
                    "sub_category": {
                      "id": "SC1",
                      "name": "Phones"
                    }
                  },
                  "total": 199.80
                }
              ],
              "shipment": {
                "carrier": "Correios",
                "service": "SEDEX",
                "status": "shipped",
                "tracking_code": "BR123"
              },
              "payment": {
                "method": "pix",
                "status": "approved",
                "transaction_id": "TXN-1"
              },
              "metadata": {
                "source": "app",
                "user_agent": "Mozilla/5.0",
                "ip_address": "1.1.1.1"
              }
            }
            """;

    @Test
    void deserializesFullPayload() throws Exception {
        OrderPayloadDto dto = mapper.readValue(FULL_JSON, OrderPayloadDto.class);

        assertThat(dto.getUuid()).isEqualTo("ORD-1");
        assertThat(dto.getCreatedAt()).isEqualTo("2025-10-01T10:15:00Z");
        assertThat(dto.getChannel()).isEqualTo("web");
        assertThat(dto.getTotal()).isEqualByComparingTo(new BigDecimal("199.80"));
        assertThat(dto.getStatus()).isEqualTo("paid");
    }

    @Test
    void deserializesCustomer() throws Exception {
        OrderPayloadDto dto = mapper.readValue(FULL_JSON, OrderPayloadDto.class);

        assertThat(dto.getCustomer()).isNotNull();
        assertThat(dto.getCustomer().getId()).isEqualTo(1);
        assertThat(dto.getCustomer().getName()).isEqualTo("Maria");
        assertThat(dto.getCustomer().getEmail()).isEqualTo("maria@test.com");
        assertThat(dto.getCustomer().getDocument()).isEqualTo("12345678900");
    }

    @Test
    void deserializesSeller() throws Exception {
        OrderPayloadDto dto = mapper.readValue(FULL_JSON, OrderPayloadDto.class);

        assertThat(dto.getSeller()).isNotNull();
        assertThat(dto.getSeller().getId()).isEqualTo(5);
        assertThat(dto.getSeller().getName()).isEqualTo("Loja X");
    }

    @Test
    void deserializesItemsWithSnakeCaseFields() throws Exception {
        OrderPayloadDto dto = mapper.readValue(FULL_JSON, OrderPayloadDto.class);

        assertThat(dto.getItems()).hasSize(1);
        ItemDto item = dto.getItems().get(0);
        assertThat(item.getProductId()).isEqualTo(101);
        assertThat(item.getProductName()).isEqualTo("Produto A");
        assertThat(item.getUnitPrice()).isEqualByComparingTo(new BigDecimal("99.90"));
        assertThat(item.getQuantity()).isEqualTo(2);
    }

    @Test
    void deserializesCategoryWithSubCategory() throws Exception {
        OrderPayloadDto dto = mapper.readValue(FULL_JSON, OrderPayloadDto.class);

        CategoryDto cat = dto.getItems().get(0).getCategory();
        assertThat(cat.getId()).isEqualTo("C1");
        assertThat(cat.getName()).isEqualTo("Electronics");
        assertThat(cat.getSubCategory()).isNotNull();
        assertThat(cat.getSubCategory().getId()).isEqualTo("SC1");
        assertThat(cat.getSubCategory().getName()).isEqualTo("Phones");
    }

    @Test
    void deserializesShipmentWithSnakeCaseTrackingCode() throws Exception {
        OrderPayloadDto dto = mapper.readValue(FULL_JSON, OrderPayloadDto.class);

        assertThat(dto.getShipment()).isNotNull();
        assertThat(dto.getShipment().getCarrier()).isEqualTo("Correios");
        assertThat(dto.getShipment().getTrackingCode()).isEqualTo("BR123");
    }

    @Test
    void deserializesPaymentWithSnakeCaseTransactionId() throws Exception {
        OrderPayloadDto dto = mapper.readValue(FULL_JSON, OrderPayloadDto.class);

        assertThat(dto.getPayment()).isNotNull();
        assertThat(dto.getPayment().getMethod()).isEqualTo("pix");
        assertThat(dto.getPayment().getTransactionId()).isEqualTo("TXN-1");
    }

    @Test
    void deserializesMetadataWithSnakeCaseFields() throws Exception {
        OrderPayloadDto dto = mapper.readValue(FULL_JSON, OrderPayloadDto.class);

        assertThat(dto.getMetadata()).isNotNull();
        assertThat(dto.getMetadata().getSource()).isEqualTo("app");
        assertThat(dto.getMetadata().getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(dto.getMetadata().getIpAddress()).isEqualTo("1.1.1.1");
    }

    @Test
    void deserializesPayloadWithoutOptionalFields() throws Exception {
        String json = """
                {
                  "uuid": "ORD-2",
                  "created_at": "2025-10-01T10:15:00Z",
                  "channel": "mobile",
                  "total": 50,
                  "status": "created",
                  "customer": {"id": 1, "name": "Test", "email": "t@t.com", "document": "111"},
                  "seller": {"id": 1, "name": "S", "city": "C", "state": "SP"},
                  "items": [{"id": 1, "product_id": 1, "product_name": "P", "unit_price": 50, "quantity": 1, "category": {"id": "C1", "name": "Cat", "sub_category": {"id": "SC1", "name": "Sub"}}, "total": 50}]
                }
                """;

        OrderPayloadDto dto = mapper.readValue(json, OrderPayloadDto.class);

        assertThat(dto.getShipment()).isNull();
        assertThat(dto.getPayment()).isNull();
        assertThat(dto.getMetadata()).isNull();
    }
}
