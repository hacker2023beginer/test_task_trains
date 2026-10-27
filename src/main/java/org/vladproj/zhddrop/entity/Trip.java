package org.vladproj.zhddrop.entity;

import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name="trips")
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "train_number", nullable = false)
    private String trainNumber;

    @ManyToOne
    @JoinColumn(name = "from_station_code")
    private Station fromStation;

    @ManyToOne
    @JoinColumn(name = "to_station_code")
    private Station toStation;

    @Column(name = "departure_time", nullable = false)
    private Instant departureTime;

    @Column(name = "arrival_time", nullable = false)
    private Instant arrivalTime;
}
