package org.vladproj.zhddrop.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.vladproj.zhddrop.dto.TripSearchResponse;
import org.vladproj.zhddrop.entity.Station;
import org.vladproj.zhddrop.entity.Trip;
import org.vladproj.zhddrop.entity.TripRate;
import org.vladproj.zhddrop.mapper.TripMapper;
import org.vladproj.zhddrop.repository.TripRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class TripService {
    private TripRepository tripRepository;
    private TripMapper tripMapper;

    public List<TripSearchResponse> searchAvailableTrips(Station fromStation, Station toStation, LocalDate date, int passengers) {
        Instant startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1).toInstant();
        Instant currentInstant = Instant.now();

        List<Trip> trips = tripRepository.findAvailableTrips(
            fromStation,
            toStation,
            startOfDay,
            endOfDay,
            currentInstant,
            passengers
        );

        return trips.stream()
            .peek(trip -> {
                List<TripRate> validRates = trip.getTripRates().stream()
                    .filter(rate -> rate.getAvailableSeats() >= passengers)
                    .collect(Collectors.toList());
                trip.setTripRates(validRates);
            })

            .sorted(Comparator.comparing(
                (Trip trip) -> trip.getTripRates().stream()
                    .map(TripRate::getPrice)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO),
                BigDecimal::compareTo
            ).thenComparing(Trip::getDepartureTime))
            .map(trip -> tripMapper.toResponse(trip))
            .collect(Collectors.toList());
    }


}
