package org.vladproj.zhddrop.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vladproj.zhddrop.config.ReservationProperties;
import org.vladproj.zhddrop.dto.ReservationRequest;
import org.vladproj.zhddrop.dto.ReservationResponse;
import org.vladproj.zhddrop.dto.TripRateResponse;
import org.vladproj.zhddrop.dto.TripSearchResponse;
import org.vladproj.zhddrop.entity.*;
import org.springframework.context.ApplicationEventPublisher;
import org.vladproj.zhddrop.event.TicketConfirmedEvent;
import org.vladproj.zhddrop.exception.ConflictException;
import org.vladproj.zhddrop.exception.NotFoundException;
import org.vladproj.zhddrop.repository.ReservationRepository;
import org.vladproj.zhddrop.repository.TripRateRepository;
import org.vladproj.zhddrop.repository.TripRepository;
import org.vladproj.zhddrop.repository.projection.ExpiredHoldView;
import org.vladproj.zhddrop.repository.projection.ReservationSeatView;
import org.vladproj.zhddrop.validator.ReserveValidator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final TripRateRepository tripRateRepository;
    private final ReservationRepository reservationRepository;
    private final ReserveValidator reserveValidator;
    private final ReservationProperties reservationProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<TripSearchResponse> searchAvailableTrips(StationCode from, StationCode to, LocalDate date, int passengers) {
        ZoneId zone = ZoneId.systemDefault();
        Instant startOfDay = date.atStartOfDay(zone).toInstant();
        Instant endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant();

        List<Trip> trips = tripRepository.findAvailableTrips(
            from.name(),
            to.name(),
            startOfDay,
            endOfDay,
            Instant.now(),
            passengers
        );

        return trips.stream()
            .map(trip -> toSearchResponse(trip, passengers))
            .filter(response -> !response.getRates().isEmpty())
            .sorted(Comparator.comparing((TripSearchResponse response) -> totalPrice(response, passengers))
                .thenComparing(TripSearchResponse::getDepartureTime))
            .toList();
    }

    @Transactional
    public ReservationResponse reserveTrip(ReservationRequest request) {
        Instant now = Instant.now();

        Trip trip = tripRepository.findById(request.getTripId())
            .orElseThrow(() -> new NotFoundException(
                "TRIP_NOT_FOUND", "Trip %d was not found".formatted(request.getTripId())));

        reserveValidator.requireNotDeparted(trip.getDepartureTime(), now);

        BigDecimal price = tripRateRepository.findPrice(trip.getId(), request.getFareCode())
            .orElseThrow(() -> new NotFoundException(
                "FARE_NOT_FOUND",
                "Fare %s is not available for trip %d".formatted(request.getFareCode(), trip.getId())));

        int takenSeats = tripRateRepository.takeSeats(trip.getId(), request.getFareCode(), request.getPassengerCount());
        if (takenSeats == 0) {
            throw new ConflictException(
                "SEATS_NOT_AVAILABLE",
                "Not enough seats for fare %s on trip %d".formatted(request.getFareCode(), trip.getId()));
        }

        Reservation reservation = new Reservation();
        reservation.setTrip(trip);
        reservation.setRateCode(request.getFareCode());
        reservation.setPassengerCount(request.getPassengerCount());
        reservation.setTotalPrice(price.multiply(BigDecimal.valueOf(request.getPassengerCount())));
        reservation.setStatus(ReservationStatus.HELD);
        reservation.setCreatedAt(now);
        reservation.setExpiresAt(now.plus(reservationProperties.ttl()));
        reservationRepository.saveAndFlush(reservation);

        return ReservationResponse.from(reservation);
    }

    @Transactional
    public void cancelReservation(Integer reservationId) {
        ReservationSeatView view = reservationRepository.findSeatView(reservationId)
            .orElseThrow(() -> reservationNotFound(reservationId));

        int updated = reservationRepository.updateStatus(
            reservationId, ReservationStatus.HELD, ReservationStatus.CANCELLED);

        if (updated == 0) {
            ReservationStatus current = reservationRepository.findStatusById(reservationId)
                .orElseThrow(() -> reservationNotFound(reservationId));
            throw new ConflictException(
                "RESERVATION_NOT_HELD",
                "Only HELD reservations can be cancelled, current status is %s".formatted(current));
        }

        tripRateRepository.releaseSeats(view.getTripId(), view.getRateCode(), view.getPassengerCount());
    }

    @Transactional(noRollbackFor = ConflictException.class)
    public void confirmReservation(Integer reservationId) {
        Instant now = Instant.now();

        ReservationSeatView view = reservationRepository.findSeatView(reservationId)
            .orElseThrow(() -> reservationNotFound(reservationId));

        int confirmed = reservationRepository.updateStatusIfNotExpired(
            reservationId, ReservationStatus.HELD, ReservationStatus.CONFIRMED, now);

        if (confirmed == 1) {
            eventPublisher.publishEvent(new TicketConfirmedEvent(
                reservationId,
                view.getTripId(),
                view.getTrainNumber(),
                view.getRateCode(),
                view.getPassengerCount(),
                view.getTotalPrice(),
                now));
            return;
        }

        ReservationStatus current = reservationRepository.findStatusById(reservationId)
            .orElseThrow(() -> reservationNotFound(reservationId));

        if (current == ReservationStatus.HELD) {
            if (reservationRepository.updateStatus(reservationId, ReservationStatus.HELD, ReservationStatus.EXPIRED) == 1) {
                tripRateRepository.releaseSeats(view.getTripId(), view.getRateCode(), view.getPassengerCount());
            }
            throw new ConflictException(
                "RESERVATION_EXPIRED", "Reservation %d has expired and cannot be confirmed".formatted(reservationId));
        }

        throw new ConflictException(
            "RESERVATION_NOT_HELD",
            "Only HELD reservations can be confirmed, current status is %s".formatted(current));
    }

    @Transactional
    public int expireHoldReservations() {
        Instant now = Instant.now();
        List<ExpiredHoldView> holds = reservationRepository.findExpiredHolds(ReservationStatus.HELD, now);

        int expired = 0;
        for (ExpiredHoldView hold : holds) {
            int updated = reservationRepository.updateStatus(
                hold.getId(), ReservationStatus.HELD, ReservationStatus.EXPIRED);
            if (updated == 1) {
                tripRateRepository.releaseSeats(hold.getTripId(), hold.getRateCode(), hold.getPassengerCount());
                expired++;
            }
        }
        return expired;
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Integer id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> reservationNotFound(id));
        return ReservationResponse.from(reservation);
    }

    private static NotFoundException reservationNotFound(Integer id) {
        return new NotFoundException("RESERVATION_NOT_FOUND", "Reservation %d was not found".formatted(id));
    }

    private static TripSearchResponse toSearchResponse(Trip trip, int passengers) {
        List<TripRateResponse> rates = trip.getTripRates().stream()
            .filter(rate -> rate.getAvailableSeats() >= passengers)
            .map(rate -> new TripRateResponse(rate.getRateCode(), rate.getPrice(), rate.getAvailableSeats()))
            .sorted(Comparator.comparing(TripRateResponse::getPrice))
            .toList();

        return new TripSearchResponse(
            trip.getTrainNumber(),
            trip.getDepartureTime(),
            trip.getArrivalTime(),
            rates
        );
    }

    private static BigDecimal totalPrice(TripSearchResponse response, int passengers) {
        return response.getRates().stream()
            .map(TripRateResponse::getPrice)
            .min(Comparator.naturalOrder())
            .orElse(BigDecimal.ZERO)
            .multiply(BigDecimal.valueOf(passengers));
    }
}
