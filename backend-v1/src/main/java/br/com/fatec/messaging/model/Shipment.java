package br.com.fatec.messaging.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "shipment")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "order_uuid", nullable = false, unique = true)
    private Orders order;

    @Column(name = "carrier")
    private String carrier;

    @Column(name = "service")
    private String service;

    @Column(name = "status")
    private String status;

    @Column(name = "tracking_code")
    private String trackingCode;

}
