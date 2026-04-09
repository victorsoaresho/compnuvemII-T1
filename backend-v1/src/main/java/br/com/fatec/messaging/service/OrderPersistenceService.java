package br.com.fatec.messaging.service;

import br.com.fatec.messaging.model.*;
import br.com.fatec.messaging.model.dto.ItemDto;
import br.com.fatec.messaging.model.dto.OrderPayloadDto;
import br.com.fatec.messaging.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
public class OrderPersistenceService {

    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final CustomerRepository customerRepository;
    private final SellerRepository sellerRepository;
    private final ProductRepository productRepository;
    private final OrdersRepository ordersRepository;
    private final ShipmentRepository shipmentRepository;
    private final PaymentRepository paymentRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderPersistenceService(CategoryRepository categoryRepository,
                                   SubCategoryRepository subCategoryRepository,
                                   CustomerRepository customerRepository,
                                   SellerRepository sellerRepository,
                                   ProductRepository productRepository,
                                   OrdersRepository ordersRepository,
                                   ShipmentRepository shipmentRepository,
                                   PaymentRepository paymentRepository,
                                   OrderItemRepository orderItemRepository) {
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.customerRepository = customerRepository;
        this.sellerRepository = sellerRepository;
        this.productRepository = productRepository;
        this.ordersRepository = ordersRepository;
        this.shipmentRepository = shipmentRepository;
        this.paymentRepository = paymentRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional
    public void persist(OrderPayloadDto dto) {
        // 1 & 2 — Category + SubCategory (per item)
        for (ItemDto item : dto.getItems()) {
            Category category = new Category(item.getCategory().getId(), item.getCategory().getName());
            categoryRepository.save(category);

            SubCategory subCategory = new SubCategory(
                    item.getCategory().getSubCategory().getId(),
                    item.getCategory().getSubCategory().getName(),
                    category);
            subCategoryRepository.save(subCategory);
        }

        // 3 — Customer (upsert by document)
        Customer customer = customerRepository.findByDocument(dto.getCustomer().getDocument())
                .orElseGet(() -> {
                    Customer c = new Customer();
                    c.setName(dto.getCustomer().getName());
                    c.setEmail(dto.getCustomer().getEmail());
                    c.setDocument(dto.getCustomer().getDocument());
                    return customerRepository.save(c);
                });

        // 4 — Seller (upsert by id)
        Seller seller = new Seller(
                dto.getSeller().getId(),
                dto.getSeller().getName(),
                dto.getSeller().getCity(),
                dto.getSeller().getState());
        sellerRepository.save(seller);

        // 5 — Products (upsert by productId, per item)
        // Also compute total
        BigDecimal computedTotal = BigDecimal.ZERO;
        for (ItemDto item : dto.getItems()) {
            computedTotal = computedTotal.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // 6 — Orders
        OrderStatus status;
        try {
            status = OrderStatus.valueOf(dto.getStatus());
        } catch (IllegalArgumentException e) {
            status = OrderStatus.created;
        }

        Orders order = new Orders();
        order.setUuid(dto.getUuid());
        order.setCreatedAt(OffsetDateTime.parse(dto.getCreatedAt()));
        order.setIndexedAt(OffsetDateTime.now());
        order.setChannel(dto.getChannel());
        order.setTotal(computedTotal);
        order.setStatus(status);
        if (dto.getMetadata() != null) {
            order.setSource(dto.getMetadata().getSource());
            order.setUserAgent(dto.getMetadata().getUserAgent());
            order.setIpAddress(dto.getMetadata().getIpAddress());
        }
        order.setCustomer(customer);
        order.setSeller(seller);
        ordersRepository.save(order);

        // 7 — Shipment
        if (dto.getShipment() != null) {
            Shipment shipment = new Shipment();
            shipment.setOrder(order);
            shipment.setCarrier(dto.getShipment().getCarrier());
            shipment.setService(dto.getShipment().getService());
            shipment.setStatus(dto.getShipment().getStatus());
            shipment.setTrackingCode(dto.getShipment().getTrackingCode());
            shipmentRepository.save(shipment);
        }

        // 8 — Payment
        if (dto.getPayment() != null) {
            Payment payment = new Payment();
            payment.setOrder(order);
            payment.setMethod(dto.getPayment().getMethod());
            payment.setStatus(dto.getPayment().getStatus());
            payment.setTransactionId(dto.getPayment().getTransactionId());
            paymentRepository.save(payment);
        }

        // 9 — OrderItems
        for (ItemDto item : dto.getItems()) {
            SubCategory subCat = subCategoryRepository.findById(item.getCategory().getSubCategory().getId())
                    .orElseThrow();

            Product product = productRepository.findByProductId(item.getProductId())
                    .orElseGet(() -> {
                        Product p = new Product();
                        p.setProductId(item.getProductId());
                        p.setProductName(item.getProductName());
                        p.setUnitPrice(item.getUnitPrice());
                        p.setSubCategory(subCat);
                        return productRepository.save(p);
                    });

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setProductName(item.getProductName());
            orderItem.setUnitPrice(item.getUnitPrice());
            orderItem.setQuantity(item.getQuantity());
            // total is GENERATED by the database — do not set
            orderItemRepository.save(orderItem);
        }
    }
}
