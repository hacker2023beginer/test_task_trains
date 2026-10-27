package org.vladproj.zhddrop.entity;

import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name="trip_rates")
public class TripRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_code", nullable = false)
    private RateCode rateCode;

    @Column(name = "price", nullable = false)
    private Double price;

    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats;
}
