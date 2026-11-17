package org.vladproj.zhddrop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vladproj.zhddrop.config.ReservationProperties;
import org.vladproj.zhddrop.dto.ReservationRequest;
import org.vladproj.zhddrop.dto.ReservationResponse;
import org.vladproj.zhddrop.dto.TripSearchResponse;
import org.vladproj.zhddrop.entity.RateCode;
import org.vladproj.zhddrop.entity.Reservation;
import org.vladproj.zhddrop.entity.ReservationStatus;
import org.vladproj.zhddrop.entity.StationCode;
import org.vladproj.zhddrop.entity.Trip;
import org.vladproj.zhddrop.entity.TripRate;
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
import java.time.Duration;
import java.util.ArrayList;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;
    @Mock
    private TripRateRepository tripRateRepository;
    @Mock
    private ReservationRepository reservationRepository;

    private TripService tripService;
    private final List<Object> publishedEvents = new ArrayList<>();

    @BeforeEach
    void setUp() {
        publishedEvents.clear();
        tripService = new TripService(
            tripRepository,
            tripRateRepository,
            reservationRepository,
            new ReserveValidator(),
            ReservationProperties.defaults(),
            publishedEvents::add
        );
    }

    @Test
    void searchAvailableTrips_filtersRatesAndSortsByTotalPrice() {
        Trip cheap = trip(101, "002A", Instant.now().plusSeconds(3600),
            rate(RateCode.PROMO, "1500.00", 2),
            rate(RateCode.STANDARD, "3500.00", 50));
        Trip expensive = trip(102, "004B", Instant.now().plusSeconds(7200),
            rate(RateCode.PROMO, "900.00", 1),
            rate(RateCode.STANDARD, "4000.00", 50));

        when(tripRepository.findAvailableTrips(eq("MSK"), eq("SPB"), any(), any(), any(), eq(2)))
            .thenReturn(List.of(expensive, cheap));

        List<TripSearchResponse> result = tripService.searchAvailableTrips(
            StationCode.MSK, StationCode.SPB, LocalDate.now(), 2);

        assertEquals(2, result.size());
        assertEquals("002A", result.get(0).getTrainNumber());
        assertEquals(2, result.get(0).getRates().size());
        assertEquals(RateCode.PROMO, result.get(0).getRates().get(0).getFareCode());
        assertEquals("004B", result.get(1).getTrainNumber());
        assertEquals(1, result.get(1).getRates().size());
        assertEquals(RateCode.STANDARD, result.get(1).getRates().get(0).getFareCode());
    }

    @Test
    void searchAvailableTrips_dropsTripsWithoutMatchingRates() {
        Trip trip = trip(101, "002A", Instant.now().plusSeconds(3600),
            rate(RateCode.PROMO, "1500.00", 1));

        when(tripRepository.findAvailableTrips(eq("MSK"), eq("SPB"), any(), any(), any(), eq(2)))
            .thenReturn(List.of(trip));

        assertTrue(tripService.searchAvailableTrips(StationCode.MSK, StationCode.SPB, LocalDate.now(), 2).isEmpty());
    }

    @Test
    void reserveTrip_takesSeatsAndReturnsHeldReservation() {
        Trip trip = trip(101, "002A", Instant.now().plusSeconds(3600));
        when(tripRepository.findById(101)).thenReturn(Optional.of(trip));
        when(tripRateRepository.findPrice(101, RateCode.PROMO)).thenReturn(Optional.of(new BigDecimal("1500.00")));
        when(tripRateRepository.takeSeats(101, RateCode.PROMO, 2)).thenReturn(1);
        when(reservationRepository.saveAndFlush(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation saved = invocation.getArgument(0);
            saved.setId(7);
            return saved;
        });

        ReservationResponse response = tripService.reserveTrip(request(101, RateCode.PROMO, 2));

        assertEquals(7, response.getId());
        assertEquals(101, response.getTripId());
        assertEquals("002A", response.getTrainNumber());
        assertEquals(RateCode.PROMO, response.getFareCode());
        assertEquals(2, response.getPassengerCount());
        assertEquals(0, new BigDecimal("3000.00").compareTo(response.getTotalPrice()));
        assertEquals(ReservationStatus.HELD, response.getStatus());
        assertEquals(Duration.ofMinutes(15), Duration.between(response.getCreatedAt(), response.getExpiresAt()));
    }

    @Test
    void reserveTrip_doesNotReserveWhenSeatsAreNotEnough() {
        Trip trip = trip(101, "002A", Instant.now().plusSeconds(3600));
        when(tripRepository.findById(101)).thenReturn(Optional.of(trip));
        when(tripRateRepository.findPrice(101, RateCode.PROMO)).thenReturn(Optional.of(new BigDecimal("1500.00")));
        when(tripRateRepository.takeSeats(101, RateCode.PROMO, 3)).thenReturn(0);

        ConflictException exception = assertThrows(ConflictException.class,
            () -> tripService.reserveTrip(request(101, RateCode.PROMO, 3)));

        assertEquals("SEATS_NOT_AVAILABLE", exception.getCode());
        verify(reservationRepository, never()).saveAndFlush(any(Reservation.class));
    }

    @Test
    void reserveTrip_throwsNotFoundWhenTripMissing() {
        when(tripRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> tripService.reserveTrip(request(999, RateCode.PROMO, 1)));
        verifyNoInteractions(tripRateRepository);
    }

    @Test
    void reserveTrip_throwsNotFoundWhenFareMissing() {
        Trip trip = trip(101, "002A", Instant.now().plusSeconds(3600));
        when(tripRepository.findById(101)).thenReturn(Optional.of(trip));
        when(tripRateRepository.findPrice(101, RateCode.PROMO)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> tripService.reserveTrip(request(101, RateCode.PROMO, 1)));

        assertEquals("FARE_NOT_FOUND", exception.getCode());
    }

    @Test
    void reserveTrip_throwsConflictWhenTripAlreadyDeparted() {
        Trip trip = trip(101, "002A", Instant.now().minusSeconds(60));
        when(tripRepository.findById(101)).thenReturn(Optional.of(trip));

        ConflictException exception = assertThrows(ConflictException.class,
            () -> tripService.reserveTrip(request(101, RateCode.PROMO, 1)));

        assertEquals("TRIP_ALREADY_DEPARTED", exception.getCode());
        verifyNoInteractions(tripRateRepository);
    }

    @Test
    void cancelReservation_returnsSeatsOnce() {
        ReservationSeatView view = seatView(1, 101, RateCode.PROMO, 2, ReservationStatus.HELD);
        when(reservationRepository.findSeatView(1)).thenReturn(Optional.of(view));
        when(reservationRepository.updateStatus(1, ReservationStatus.HELD, ReservationStatus.CANCELLED)).thenReturn(1);

        tripService.cancelReservation(1);

        verify(tripRateRepository).releaseSeats(101, RateCode.PROMO, 2);
    }

    @Test
    void cancelReservation_throwsConflictWhenNotHeld() {
        ReservationSeatView view = seatView(1, 101, RateCode.PROMO, 2, ReservationStatus.CANCELLED);
        when(reservationRepository.findSeatView(1)).thenReturn(Optional.of(view));
        when(reservationRepository.updateStatus(1, ReservationStatus.HELD, ReservationStatus.CANCELLED)).thenReturn(0);
        when(reservationRepository.findStatusById(1)).thenReturn(Optional.of(ReservationStatus.CONFIRMED));

        ConflictException exception = assertThrows(ConflictException.class, () -> tripService.cancelReservation(1));

        assertEquals("RESERVATION_NOT_HELD", exception.getCode());
        verify(tripRateRepository, never()).releaseSeats(any(), any(), any(Integer.class));
    }

    @Test
    void cancelReservation_throwsNotFoundWhenReservationMissing() {
        when(reservationRepository.findSeatView(404)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> tripService.cancelReservation(404));
        verifyNoInteractions(tripRateRepository);
    }

    @Test
    void confirmReservation_confirmsActiveHold() {
        ReservationSeatView view = seatView(1, 101, RateCode.PROMO, 2, ReservationStatus.HELD);
        when(reservationRepository.findSeatView(1)).thenReturn(Optional.of(view));
        when(reservationRepository.updateStatusIfNotExpired(
            eq(1), eq(ReservationStatus.HELD), eq(ReservationStatus.CONFIRMED), any(Instant.class))).thenReturn(1);

        tripService.confirmReservation(1);

        verify(tripRateRepository, never()).releaseSeats(any(), any(), any(Integer.class));
        assertEquals(1, publishedEvents.size());
        TicketConfirmedEvent event = (TicketConfirmedEvent) publishedEvents.get(0);
        assertEquals(1, event.reservationId());
        assertEquals(101, event.tripId());
        assertEquals("002A", event.trainNumber());
        assertEquals(RateCode.PROMO, event.fareCode());
    }

    @Test
    void confirmReservation_expiresTimedOutHoldAndReturnsSeats() {
        ReservationSeatView view = seatView(1, 101, RateCode.PROMO, 2, ReservationStatus.HELD);
        when(reservationRepository.findSeatView(1)).thenReturn(Optional.of(view));
        when(reservationRepository.updateStatusIfNotExpired(
            eq(1), eq(ReservationStatus.HELD), eq(ReservationStatus.CONFIRMED), any(Instant.class))).thenReturn(0);
        when(reservationRepository.findStatusById(1)).thenReturn(Optional.of(ReservationStatus.HELD));
        when(reservationRepository.updateStatus(1, ReservationStatus.HELD, ReservationStatus.EXPIRED)).thenReturn(1);

        ConflictException exception = assertThrows(ConflictException.class, () -> tripService.confirmReservation(1));

        assertEquals("RESERVATION_EXPIRED", exception.getCode());
        verify(tripRateRepository).releaseSeats(101, RateCode.PROMO, 2);
    }

    @Test
    void expireHoldReservations_isIdempotent() {
        ExpiredHoldView hold = hold(5, 101, RateCode.PROMO, 2);
        List<ExpiredHoldView> holds = List.of(hold);

        when(reservationRepository.findExpiredHolds(eq(ReservationStatus.HELD), any(Instant.class)))
            .thenReturn(holds)
            .thenReturn(List.of());
        when(reservationRepository.updateStatus(5, ReservationStatus.HELD, ReservationStatus.EXPIRED)).thenReturn(1);

        assertEquals(1, tripService.expireHoldReservations());
        assertEquals(0, tripService.expireHoldReservations());

        verify(tripRateRepository, times(1)).releaseSeats(101, RateCode.PROMO, 2);
    }

    @Test
    void expireHoldReservations_doesNotReleaseSeatsWhenStatusChangedConcurrently() {
        ExpiredHoldView hold = hold(5, 101, RateCode.PROMO, 2);
        List<ExpiredHoldView> holds = List.of(hold);

        when(reservationRepository.findExpiredHolds(eq(ReservationStatus.HELD), any(Instant.class)))
            .thenReturn(holds);
        when(reservationRepository.updateStatus(5, ReservationStatus.HELD, ReservationStatus.EXPIRED)).thenReturn(0);

        assertEquals(0, tripService.expireHoldReservations());
        verifyNoInteractions(tripRateRepository);
    }

    private static Trip trip(Integer id, String trainNumber, Instant departureTime, TripRate... rates) {
        Trip trip = new Trip();
        trip.setId(id);
        trip.setTrainNumber(trainNumber);
        trip.setDepartureTime(departureTime);
        trip.setArrivalTime(departureTime.plusSeconds(4 * 3600));
        trip.setTripRates(List.of(rates));
        for (TripRate rate : rates) {
            rate.setTrip(trip);
        }
        return trip;
    }

    private static TripRate rate(RateCode rateCode, String price, int availableSeats) {
        TripRate rate = new TripRate();
        rate.setRateCode(rateCode);
        rate.setPrice(new BigDecimal(price));
        rate.setAvailableSeats(availableSeats);
        return rate;
    }

    private static ReservationRequest request(Integer tripId, RateCode fareCode, Integer passengerCount) {
        ReservationRequest request = new ReservationRequest();
        request.setTripId(tripId);
        request.setFareCode(fareCode);
        request.setPassengerCount(passengerCount);
        return request;
    }

    private static ReservationSeatView seatView(Integer id, Integer tripId, RateCode fareCode,
                                                Integer passengerCount, ReservationStatus status) {
        // lenient: набор читаемых геттеров зависит от сценария
        ReservationSeatView view = mock(ReservationSeatView.class);
        lenient().when(view.getId()).thenReturn(id);
        lenient().when(view.getTripId()).thenReturn(tripId);
        lenient().when(view.getTrainNumber()).thenReturn("002A");
        lenient().when(view.getRateCode()).thenReturn(fareCode);
        lenient().when(view.getPassengerCount()).thenReturn(passengerCount);
        lenient().when(view.getTotalPrice()).thenReturn(new BigDecimal("3000.00"));
        lenient().when(view.getStatus()).thenReturn(status);
        return view;
    }

    private static ExpiredHoldView hold(Integer id, Integer tripId, RateCode fareCode, Integer passengerCount) {
        ExpiredHoldView view = mock(ExpiredHoldView.class);
        when(view.getId()).thenReturn(id);
        lenient().when(view.getTripId()).thenReturn(tripId);
        lenient().when(view.getRateCode()).thenReturn(fareCode);
        lenient().when(view.getPassengerCount()).thenReturn(passengerCount);
        return view;
    }
}
