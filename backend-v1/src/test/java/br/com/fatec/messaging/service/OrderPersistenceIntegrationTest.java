package br.com.fatec.messaging.service;

import br.com.fatec.messaging.model.*;
import br.com.fatec.messaging.model.dto.*;
import br.com.fatec.messaging.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(OrderPersistenceService.class)
@ActiveProfiles("test")
class OrderPersistenceIntegrationTest {

    @Autowired
    private OrderPersistenceService service;

    @Autowired
    private OrdersRepository ordersRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private SellerRepository sellerRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private SubCategoryRepository subCategoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private ShipmentRepository shipmentRepository;
    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void persist_savesAllEntitiesToDatabase() {
        OrderPayloadDto dto = buildPayload();

        service.persist(dto);

        assertThat(categoryRepository.findAll()).hasSize(1);
        assertThat(subCategoryRepository.findAll()).hasSize(1);
        assertThat(customerRepository.findAll()).hasSize(1);
        assertThat(sellerRepository.findAll()).hasSize(1);
        assertThat(productRepository.findAll()).hasSize(1);
        assertThat(ordersRepository.findAll()).hasSize(1);
        assertThat(orderItemRepository.findAll()).hasSize(1);
        assertThat(shipmentRepository.findAll()).hasSize(1);
        assertThat(paymentRepository.findAll()).hasSize(1);

        Orders savedOrder = ordersRepository.findById("ORD-INT-001").orElseThrow();
        assertThat(savedOrder.getChannel()).isEqualTo("web");
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.paid);
        assertThat(savedOrder.getCustomer().getDocument()).isEqualTo("99988877766");
        assertThat(savedOrder.getSeller().getName()).isEqualTo("Loja Teste");
    }

    @Test
    void persist_upsertsCustomerByDocument() {
        OrderPayloadDto dto1 = buildPayload();
        service.persist(dto1);

        OrderPayloadDto dto2 = buildPayload();
        dto2.setUuid("ORD-INT-002");
        service.persist(dto2);

        assertThat(customerRepository.findAll()).hasSize(1);
        assertThat(ordersRepository.findAll()).hasSize(2);
    }

    @Test
    void persist_upsertsProductByProductId() {
        OrderPayloadDto dto1 = buildPayload();
        service.persist(dto1);

        OrderPayloadDto dto2 = buildPayload();
        dto2.setUuid("ORD-INT-003");
        service.persist(dto2);

        assertThat(productRepository.findAll()).hasSize(1);
        assertThat(orderItemRepository.findAll()).hasSize(2);
    }

    @Test
    void persist_handlesMultipleItems() {
        OrderPayloadDto dto = buildPayload();

        ItemDto item2 = new ItemDto();
        item2.setId(2);
        item2.setProductId(202);
        item2.setProductName("Second Product");
        item2.setUnitPrice(new BigDecimal("25.00"));
        item2.setQuantity(3);
        item2.setTotal(new BigDecimal("75.00"));

        SubCategoryDto subCat2 = new SubCategoryDto();
        subCat2.setId("SC2");
        subCat2.setName("SubCat2");

        CategoryDto cat2 = new CategoryDto();
        cat2.setId("C2");
        cat2.setName("Cat2");
        cat2.setSubCategory(subCat2);
        item2.setCategory(cat2);

        dto.setItems(List.of(dto.getItems().get(0), item2));

        service.persist(dto);

        assertThat(categoryRepository.findAll()).hasSize(2);
        assertThat(subCategoryRepository.findAll()).hasSize(2);
        assertThat(productRepository.findAll()).hasSize(2);
        assertThat(orderItemRepository.findAll()).hasSize(2);

        Orders order = ordersRepository.findById("ORD-INT-001").orElseThrow();
        // total should be computed: (49.90 * 2) + (25.00 * 3) = 99.80 + 75.00 = 174.80
        assertThat(order.getTotal()).isEqualByComparingTo(new BigDecimal("174.80"));
    }

    private OrderPayloadDto buildPayload() {
        SubCategoryDto subCategoryDto = new SubCategoryDto();
        subCategoryDto.setId("SC1");
        subCategoryDto.setName("SubCat");

        CategoryDto categoryDto = new CategoryDto();
        categoryDto.setId("C1");
        categoryDto.setName("Cat");
        categoryDto.setSubCategory(subCategoryDto);

        ItemDto item = new ItemDto();
        item.setId(1);
        item.setProductId(201);
        item.setProductName("Produto Teste");
        item.setUnitPrice(new BigDecimal("49.90"));
        item.setQuantity(2);
        item.setCategory(categoryDto);
        item.setTotal(new BigDecimal("99.80"));

        CustomerDto customerDto = new CustomerDto();
        customerDto.setId(1);
        customerDto.setName("Maria Silva");
        customerDto.setEmail("maria@test.com");
        customerDto.setDocument("99988877766");

        SellerDto sellerDto = new SellerDto();
        sellerDto.setId(10);
        sellerDto.setName("Loja Teste");
        sellerDto.setCity("Campinas");
        sellerDto.setState("SP");

        ShipmentDto shipmentDto = new ShipmentDto();
        shipmentDto.setCarrier("JadLog");
        shipmentDto.setService("Expresso");
        shipmentDto.setStatus("processing");
        shipmentDto.setTrackingCode("JL999888777");

        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setMethod("credit_card");
        paymentDto.setStatus("approved");
        paymentDto.setTransactionId("PAY-INT-001");

        MetadataDto metadataDto = new MetadataDto();
        metadataDto.setSource("web");
        metadataDto.setUserAgent("TestAgent");
        metadataDto.setIpAddress("10.0.0.1");

        OrderPayloadDto dto = new OrderPayloadDto();
        dto.setUuid("ORD-INT-001");
        dto.setCreatedAt("2025-10-01T10:15:00Z");
        dto.setChannel("web");
        dto.setTotal(new BigDecimal("99.80"));
        dto.setStatus("paid");
        dto.setCustomer(customerDto);
        dto.setSeller(sellerDto);
        dto.setItems(List.of(item));
        dto.setShipment(shipmentDto);
        dto.setPayment(paymentDto);
        dto.setMetadata(metadataDto);

        return dto;
    }
}
