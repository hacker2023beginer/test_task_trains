package org.vladproj.zhddrop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name="trip_rates")
@Getter
@Setter
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
    private BigDecimal price;

    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats;
}
