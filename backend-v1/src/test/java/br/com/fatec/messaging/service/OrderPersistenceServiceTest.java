package br.com.fatec.messaging.service;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import br.com.fatec.messaging.model.*;
import br.com.fatec.messaging.model.dto.*;
import br.com.fatec.messaging.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

class OrderPersistenceServiceTest {

    private CategoryRepository categoryRepository;
    private SubCategoryRepository subCategoryRepository;
    private CustomerRepository customerRepository;
    private SellerRepository sellerRepository;
    private ProductRepository productRepository;
    private OrdersRepository ordersRepository;
    private ShipmentRepository shipmentRepository;
    private PaymentRepository paymentRepository;
    private OrderItemRepository orderItemRepository;

    private OrderPersistenceService service;

    @BeforeEach
    void setUp() {
        categoryRepository = mock(CategoryRepository.class);
        subCategoryRepository = mock(SubCategoryRepository.class);
        customerRepository = mock(CustomerRepository.class);
        sellerRepository = mock(SellerRepository.class);
        productRepository = mock(ProductRepository.class);
        ordersRepository = mock(OrdersRepository.class);
        shipmentRepository = mock(ShipmentRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        orderItemRepository = mock(OrderItemRepository.class);

        service = new OrderPersistenceService(
                categoryRepository, subCategoryRepository, customerRepository,
                sellerRepository, productRepository, ordersRepository,
                shipmentRepository, paymentRepository, orderItemRepository);
    }

    @Test
    void persist_savesAllEntitiesForCompletePayload() {
        OrderPayloadDto dto = buildFullPayload();

        when(customerRepository.findByDocument("12345678900")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(inv -> inv.getArgument(0));
        when(subCategoryRepository.findById("SC1")).thenReturn(Optional.of(new SubCategory("SC1", "SubCat", new Category("C1", "Cat"))));
        when(productRepository.findByProductId(101)).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        service.persist(dto);

        verify(categoryRepository).save(any(Category.class));
        verify(subCategoryRepository).save(any(SubCategory.class));
        verify(customerRepository).findByDocument("12345678900");
        verify(customerRepository).save(any(Customer.class));
        verify(sellerRepository).save(any(Seller.class));
        verify(ordersRepository).save(any(Orders.class));
        verify(shipmentRepository).save(any(Shipment.class));
        verify(paymentRepository).save(any(Payment.class));
        verify(orderItemRepository).save(any(OrderItem.class));
    }

    @Test
    void persist_skipsShipmentWhenNull() {
        OrderPayloadDto dto = buildFullPayload();
        dto.setShipment(null);

        when(customerRepository.findByDocument("12345678900")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(inv -> inv.getArgument(0));
        when(subCategoryRepository.findById("SC1")).thenReturn(Optional.of(new SubCategory("SC1", "SubCat", new Category("C1", "Cat"))));
        when(productRepository.findByProductId(101)).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        service.persist(dto);

        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void persist_skipsPaymentWhenNull() {
        OrderPayloadDto dto = buildFullPayload();
        dto.setPayment(null);

        when(customerRepository.findByDocument("12345678900")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(inv -> inv.getArgument(0));
        when(subCategoryRepository.findById("SC1")).thenReturn(Optional.of(new SubCategory("SC1", "SubCat", new Category("C1", "Cat"))));
        when(productRepository.findByProductId(101)).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        service.persist(dto);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void persist_reusesExistingCustomerByDocument() {
        OrderPayloadDto dto = buildFullPayload();
        Customer existing = new Customer(99, "Existing", "old@test.com", "12345678900");

        when(customerRepository.findByDocument("12345678900")).thenReturn(Optional.of(existing));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(inv -> inv.getArgument(0));
        when(subCategoryRepository.findById("SC1")).thenReturn(Optional.of(new SubCategory("SC1", "SubCat", new Category("C1", "Cat"))));
        when(productRepository.findByProductId(101)).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        service.persist(dto);

        verify(customerRepository).findByDocument("12345678900");
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void persist_reusesExistingProductByProductId() {
        OrderPayloadDto dto = buildFullPayload();
        Product existingProduct = new Product(1, 101, "Existing Product", BigDecimal.TEN, null);

        when(customerRepository.findByDocument("12345678900")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(inv -> inv.getArgument(0));
        when(subCategoryRepository.findById("SC1")).thenReturn(Optional.of(new SubCategory("SC1", "SubCat", new Category("C1", "Cat"))));
        when(productRepository.findByProductId(101)).thenReturn(Optional.of(existingProduct));

        service.persist(dto);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void persist_defaultsToCreatedStatusForInvalidStatus() {
        OrderPayloadDto dto = buildFullPayload();
        dto.setStatus("invalid_status");

        when(customerRepository.findByDocument("12345678900")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(inv -> inv.getArgument(0));
        when(subCategoryRepository.findById("SC1")).thenReturn(Optional.of(new SubCategory("SC1", "SubCat", new Category("C1", "Cat"))));
        when(productRepository.findByProductId(101)).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        service.persist(dto);

        verify(ordersRepository).save(argThat(order -> order.getStatus() == OrderStatus.created));
    }

    @Test
    void persist_computesTotalFromItems() {
        OrderPayloadDto dto = buildFullPayload();
        // item: unitPrice=49.90, quantity=2 => total = 99.80

        when(customerRepository.findByDocument("12345678900")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(inv -> inv.getArgument(0));
        when(subCategoryRepository.findById("SC1")).thenReturn(Optional.of(new SubCategory("SC1", "SubCat", new Category("C1", "Cat"))));
        when(productRepository.findByProductId(101)).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        service.persist(dto);

        verify(ordersRepository).save(argThat(order ->
                order.getTotal().compareTo(new BigDecimal("99.80")) == 0));
    }

    @Test
    void persist_setsMetadataOnOrder() {
        OrderPayloadDto dto = buildFullPayload();

        when(customerRepository.findByDocument("12345678900")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(inv -> inv.getArgument(0));
        when(subCategoryRepository.findById("SC1")).thenReturn(Optional.of(new SubCategory("SC1", "SubCat", new Category("C1", "Cat"))));
        when(productRepository.findByProductId(101)).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        service.persist(dto);

        verify(ordersRepository).save(argThat(order ->
                "mobile-app".equals(order.getSource()) &&
                "Mozilla/5.0".equals(order.getUserAgent()) &&
                "192.168.1.1".equals(order.getIpAddress())));
    }

    @Test
    void persist_skipsMetadataWhenNull() {
        OrderPayloadDto dto = buildFullPayload();
        dto.setMetadata(null);

        when(customerRepository.findByDocument("12345678900")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(inv -> inv.getArgument(0));
        when(subCategoryRepository.findById("SC1")).thenReturn(Optional.of(new SubCategory("SC1", "SubCat", new Category("C1", "Cat"))));
        when(productRepository.findByProductId(101)).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        service.persist(dto);

        verify(ordersRepository).save(argThat(order ->
                order.getSource() == null &&
                order.getUserAgent() == null &&
                order.getIpAddress() == null));
    }

    private OrderPayloadDto buildFullPayload() {
        SubCategoryDto subCategoryDto = new SubCategoryDto();
        subCategoryDto.setId("SC1");
        subCategoryDto.setName("SubCat");

        CategoryDto categoryDto = new CategoryDto();
        categoryDto.setId("C1");
        categoryDto.setName("Cat");
        categoryDto.setSubCategory(subCategoryDto);

        ItemDto item = new ItemDto();
        item.setId(1);
        item.setProductId(101);
        item.setProductName("Test Product");
        item.setUnitPrice(new BigDecimal("49.90"));
        item.setQuantity(2);
        item.setCategory(categoryDto);
        item.setTotal(new BigDecimal("99.80"));

        CustomerDto customerDto = new CustomerDto();
        customerDto.setId(1);
        customerDto.setName("John Doe");
        customerDto.setEmail("john@test.com");
        customerDto.setDocument("12345678900");

        SellerDto sellerDto = new SellerDto();
        sellerDto.setId(1);
        sellerDto.setName("Seller One");
        sellerDto.setCity("Sao Paulo");
        sellerDto.setState("SP");

        ShipmentDto shipmentDto = new ShipmentDto();
        shipmentDto.setCarrier("Correios");
        shipmentDto.setService("SEDEX");
        shipmentDto.setStatus("shipped");
        shipmentDto.setTrackingCode("BR123456789");

        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setMethod("pix");
        paymentDto.setStatus("approved");
        paymentDto.setTransactionId("TXN-001");

        MetadataDto metadataDto = new MetadataDto();
        metadataDto.setSource("mobile-app");
        metadataDto.setUserAgent("Mozilla/5.0");
        metadataDto.setIpAddress("192.168.1.1");

        OrderPayloadDto dto = new OrderPayloadDto();
        dto.setUuid("ORD-TEST-001");
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
