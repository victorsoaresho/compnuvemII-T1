package br.com.fatec.messaging.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Orders {

    @Id
    private String uuid;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "indexed_at", nullable = false)
    private LocalDateTime indexedAt;

    @Column(name = "channel")
    private String channel;

    @Column(name = "total")
    private BigDecimal total;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "source")
    private String source;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "ip_address")
    private String ipAddress;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;
}
