package org.vladproj.zhddrop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vladproj.zhddrop.dto.ReservationResponse;
import org.vladproj.zhddrop.entity.*;
import org.vladproj.zhddrop.repository.ReservationRepository;
import org.vladproj.zhddrop.repository.TripRateRepository;
import org.vladproj.zhddrop.repository.TripRepository;
import org.vladproj.zhddrop.validator.ReserveValidator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;
    @Mock
    private TripRateRepository tripRateRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private ReserveValidator reserveValidator;

    @InjectMocks
    private TripService tripService;

    private Trip trip;
    private TripRate tripRate;

    @BeforeEach
    void setUp() {
        trip = new Trip();
        trip.setId(101);
        trip.setTrainNumber("002A");
        trip.setDepartureTime(Instant.now().plusSeconds(3600));

        tripRate = new TripRate();
        tripRate.setId(1);
        tripRate.setTrip(trip);
        tripRate.setRateCode(RateCode.PROMO);
        tripRate.setPrice(BigDecimal.valueOf(1500));
        tripRate.setAvailableSeats(5);
    }

    @Test
    void reserveTrip_Success() {
        when(tripRepository.findById(101)).thenReturn(Optional.of(trip));
        when(tripRateRepository.findForUpdate(101, RateCode.PROMO)).thenReturn(Optional.of(tripRate));
        when(tripRateRepository.getTripRateByTrip_IdAndRateCode(101, RateCode.PROMO))
            .thenReturn(Optional.of(tripRate));
        doNothing().when(reserveValidator).isOverallPassengers(any(), any());
        doNothing().when(reserveValidator).isMatchedByDate(any(), any());

        ReservationResponse response = tripService.reserveTrip(101, RateCode.PROMO, 2);

        assertNotNull(response);
        assertEquals("002A", response.getTrainNumber());
        assertEquals("HELD", response.getStatus());
        assertEquals(3, tripRate.getAvailableSeats());
        verify(reservationRepository, times(1)).save(any(Reservation.class));
    }

    @Test
    void cancelReservation_Success() {
        Reservation reservation = new Reservation();
        reservation.setId(1);
        reservation.setTrip(trip);
        reservation.setRateCode("PROMO");
        reservation.setPassengerCount(2);
        reservation.setStatus(ReservationStatus.HELD);

        when(reservationRepository.findById(1)).thenReturn(Optional.of(reservation));
        when(tripRateRepository.getTripRateByTrip_IdAndRateCode(101, RateCode.PROMO))
            .thenReturn(Optional.of(tripRate));

        tripService.cancelReservation(1);

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(7, tripRate.getAvailableSeats()); // Места вернулись (5 + 2 = 7)
        verify(reservationRepository, times(1)).save(reservation);
    }

    @Test
    void confirmReservation_Success() {
        Reservation reservation = new Reservation();
        reservation.setId(1);
        reservation.setStatus(ReservationStatus.HELD);
        reservation.setExpiresAt(Instant.now().plusSeconds(600));

        when(reservationRepository.findById(1)).thenReturn(Optional.of(reservation));

        tripService.confirmReservation(1);

        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        verify(reservationRepository, times(1)).save(reservation);
    }
}