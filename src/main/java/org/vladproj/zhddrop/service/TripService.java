package org.vladproj.zhddrop.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vladproj.zhddrop.dto.ReservationResponse;
import org.vladproj.zhddrop.dto.TripSearchResponse;
import org.vladproj.zhddrop.entity.*;
import org.vladproj.zhddrop.exception.TripServiceException;
import org.vladproj.zhddrop.mapper.TripMapper;
import org.vladproj.zhddrop.repository.ReservationRepository;
import org.vladproj.zhddrop.repository.TripRateRepository;
import org.vladproj.zhddrop.repository.TripRepository;
import org.vladproj.zhddrop.validator.ReserveValidator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class TripService {
    private static final long RESERVE_TIME_IN_SECONDS = 900;
    private TripRepository tripRepository;
    private TripRateRepository tripRateRepository;
    private ReservationRepository reservationRepository;
    private ReserveValidator reserveValidator;
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
                    .toList();
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
            .toList();
    }

    @Transactional
    public ReservationResponse reserveTrip(Integer id, RateCode rateCode, Integer passengers) {
        Trip trip = tripRepository.findById(id).orElseThrow(() -> new TripServiceException("Trip was not found by this id"));
        TripRate tripRate = tripRateRepository.findForUpdate(id, rateCode).orElseThrow(() -> new TripServiceException("Trip rate was not found by this code"));
        reserveValidator.isOverallPassengers(tripRate.getAvailableSeats(), passengers);
        reserveValidator.isMatchedByDate(trip.getDepartureTime(), Instant.now());

        Reservation reservation = new Reservation();
        Instant reservationDate = Instant.now();
        reservation.setCreatedAt(reservationDate);
        reservation.setExpiresAt(reservationDate.plusSeconds(RESERVE_TIME_IN_SECONDS));
        reservation.setPassengerCount(passengers);
        reservation.setRateCode(rateCode.name());
        reservation.setStatus(ReservationStatus.HELD);
        reservation.setTotalPrice(tripRate.getPrice().multiply(new BigDecimal(passengers)));
        reservation.setTrip(trip);
        reservationRepository.save(reservation);

        tripRate.setAvailableSeats(tripRate.getAvailableSeats() - passengers);
        tripRateRepository.save(tripRate);
        return new ReservationResponse(trip.getTrainNumber(), rateCode.name(), passengers, reservationDate, reservation.getStatus().name());
    }

    @Transactional
    public void cancelReservation(Integer reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new TripServiceException("Reservation not found"));

        if (reservation.getStatus() != ReservationStatus.HELD) {
            throw new TripServiceException("Only HELD reservations can be cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        TripRate tripRate = tripRateRepository.getTripRateByTrip_IdAndRateCode(
            reservation.getTrip().getId(),
            RateCode.valueOf(reservation.getRateCode())
        ).orElseThrow(() -> new TripServiceException("Trip rate not found"));

        tripRate.setAvailableSeats(tripRate.getAvailableSeats() + reservation.getPassengerCount());
        tripRateRepository.save(tripRate);
    }

    @Transactional
    public void expireHoldReservations() {
        Instant now = Instant.now();
        List<Reservation> expiredReservations = reservationRepository.findByStatusAndExpiresAtBefore(ReservationStatus.HELD, now);

        for (Reservation reservation : expiredReservations) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);

            tripRateRepository.getTripRateByTrip_IdAndRateCode(
                reservation.getTrip().getId(),
                RateCode.valueOf(reservation.getRateCode())
            ).ifPresent(tripRate -> {
                tripRate.setAvailableSeats(tripRate.getAvailableSeats() + reservation.getPassengerCount());
                tripRateRepository.save(tripRate);
            });
        }
    }

    public ReservationResponse getReservationById(Integer id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new TripServiceException("Reservation was not found by this id"));
        if (reservation.getStatus() == ReservationStatus.HELD && reservation.getExpiresAt().isBefore(Instant.now())) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);
        }

        return new ReservationResponse(
            reservation.getTrip().getTrainNumber(),
            reservation.getRateCode(),
            reservation.getPassengerCount(),
            reservation.getCreatedAt(),
            reservation.getStatus().name()
        );
    }

    @Transactional
    public void confirmReservation(Integer reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new TripServiceException("Reservation was not found"));

        if (reservation.getStatus() != ReservationStatus.HELD) {
            throw new TripServiceException("Only HELD reservations can be confirmed");
        }

        if (reservation.getExpiresAt().isBefore(Instant.now())) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);
            throw new TripServiceException("Reservation has expired and cannot be confirmed");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);
    }
}
