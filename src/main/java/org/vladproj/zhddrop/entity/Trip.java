package org.vladproj.zhddrop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.List;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name="trips")
@Getter
@Setter
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "train_number", nullable = false)
    private String trainNumber;

    @OneToMany(mappedBy = "trip", fetch = FetchType.LAZY)
    private List<TripRate> tripRates;

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
