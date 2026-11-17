package org.vladproj.zhddrop.it;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ReservationFlowIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:16-alpine"));

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(
        DockerImageName.parse("apache/kafka:3.8.0"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.kafka.producer.properties.max.block.ms", () -> "5000");

        registry.add("app.kafka.enabled", () -> "true");
        registry.add("app.kafka.topic", () -> "ticket-confirmed");
    }

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetDemoData() {
        jdbcTemplate.execute("DELETE FROM reservations");
        jdbcTemplate.execute("UPDATE trip_rates SET available_seats = 2 WHERE trip_id = 101 AND rate_code = 'PROMO'");
        jdbcTemplate.execute("UPDATE trip_rates SET available_seats = 50 WHERE trip_id = 101 AND rate_code = 'STANDARD'");
        jdbcTemplate.execute("UPDATE trip_rates SET available_seats = 10 WHERE trip_id = 102 AND rate_code = 'PROMO'");
        jdbcTemplate.execute("UPDATE trip_rates SET available_seats = 3 WHERE trip_id = 103 AND rate_code = 'PROMO'");
    }

    @Test
    void holdConfirmCancelFlowWithKafkaEvent() {
        LocalDate departureDate = LocalDate.now().plusDays(1);
        String searchUrl = "/api/trips/search?from=MSK&to=SPB&date=%s&passengers=2"
            .formatted(departureDate);

        ResponseEntity<String> search = rest.getForEntity(searchUrl, String.class);
        assertEquals(HttpStatus.OK, search.getStatusCode());
        assertTrue(search.getBody().contains("\"trainNumber\":\"002A\""),
            "need train 002A: " + search.getBody());

        ResponseEntity<String> tooMany = rest.postForEntity(
            "/api/reservations", body(101, "PROMO", 3), String.class);
        assertEquals(HttpStatus.CONFLICT, tooMany.getStatusCode());
        assertTrue(tooMany.getBody().contains("SEATS_NOT_AVAILABLE"), tooMany.getBody());

        ResponseEntity<Map> created = rest.postForEntity(
            "/api/reservations", body(101, "PROMO", 2), Map.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        Map<?, ?> reservation = created.getBody();
        assertNotNull(reservation);
        Integer reservationId = (Integer) reservation.get("id");
        assertEquals("HELD", reservation.get("status"));
        assertEquals("3000.00", String.valueOf(reservation.get("totalPrice")));

        assertTrue(!seatsOf(101, "PROMO", departureDate).contains("\"fareCode\":\"PROMO\""),
            "fare without available seats must not being in search");

        try (KafkaConsumer<String, String> consumer = consumer()) {
            consumer.subscribe(java.util.List.of("ticket-confirmed"));

            ResponseEntity<String> confirmed = rest.exchange(
                "/api/reservations/" + reservationId + "/confirm",
                HttpMethod.PUT, HttpEntity.EMPTY, String.class);
            assertEquals(HttpStatus.OK, confirmed.getStatusCode());

            assertEquals("CONFIRMED", statusOf(reservationId));
            assertTrue(readTicketConfirmed(consumer, reservationId),
                "event TicketConfirmed was not send to Kafka");
        }

        ResponseEntity<String> cancelConfirmed = rest.exchange(
            "/api/reservations/" + reservationId + "/cancel",
            HttpMethod.PUT, HttpEntity.EMPTY, String.class);
        assertEquals(HttpStatus.CONFLICT, cancelConfirmed.getStatusCode());
        assertTrue(cancelConfirmed.getBody().contains("RESERVATION_NOT_HELD"), cancelConfirmed.getBody());

        ResponseEntity<Map> second = rest.postForEntity(
            "/api/reservations", body(101, "PROMO", 2), Map.class);
        assertEquals(HttpStatus.CREATED, second.getStatusCode());
        Integer secondId = (Integer) second.getBody().get("id");

        ResponseEntity<String> cancelled = rest.exchange(
            "/api/reservations/" + secondId + "/cancel",
            HttpMethod.PUT, HttpEntity.EMPTY, String.class);
        assertEquals(HttpStatus.OK, cancelled.getStatusCode());
        assertEquals("CANCELLED", statusOf(secondId));
        assertTrue(seatsOf(101, "PROMO", departureDate).contains("\"availableSeats\":2"));
    }

    @Test
    void unknownStationReturns400() {
        ResponseEntity<String> response = rest.getForEntity(
            "/api/trips/search?from=XXX&to=SPB&date=" + LocalDate.now().plusDays(1) + "&passengers=2",
            String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("UNKNOWN_STATION"), response.getBody());
    }

    @Test
    void cancelledHoldCannotBeConfirmed() {
        ResponseEntity<Map> created = rest.postForEntity(
            "/api/reservations", body(103, "PROMO", 1), Map.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        Integer reservationId = (Integer) created.getBody().get("id");

        ResponseEntity<String> expired = rest.exchange(
            "/api/reservations/" + reservationId + "/cancel",
            HttpMethod.PUT, HttpEntity.EMPTY, String.class);
        assertEquals(HttpStatus.OK, expired.getStatusCode());

        ResponseEntity<String> afterExpiry = rest.exchange(
            "/api/reservations/" + reservationId + "/confirm",
            HttpMethod.PUT, HttpEntity.EMPTY, String.class);
        assertEquals(HttpStatus.CONFLICT, afterExpiry.getStatusCode());
        assertTrue(afterExpiry.getBody().contains("RESERVATION_NOT_HELD"), afterExpiry.getBody());
    }

    private Map<String, Object> body(int tripId, String fareCode, int passengerCount) {
        Map<String, Object> body = new HashMap<>();
        body.put("tripId", tripId);
        body.put("fareCode", fareCode);
        body.put("passengerCount", passengerCount);
        return body;
    }

    private String seatsOf(int tripId, String fareCode, LocalDate date) {
        String url = "/api/trips/search?from=MSK&to=SPB&date=%s&passengers=1".formatted(date);
        String response = rest.getForObject(url, String.class);
        int tripIndex = response.indexOf("\"trainNumber\":\"" + (tripId == 101 ? "002A" : "002B") + "\"");
        assertTrue(tripIndex >= 0, "поезд не найден в выдаче: " + response);
        return response.substring(tripIndex);
    }

    private String statusOf(Integer reservationId) {
        Map<?, ?> reservation = rest.getForObject("/api/reservations/" + reservationId, Map.class);
        assertNotNull(reservation);
        return String.valueOf(reservation.get("status"));
    }

    private KafkaConsumer<String, String> consumer() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "it-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(properties);
    }

    private boolean readTicketConfirmed(KafkaConsumer<String, String> consumer, Integer reservationId) {
        String marker = "\"reservationId\":" + reservationId;
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(20).toMillis();
        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
            for (ConsumerRecord<String, String> recordElement : records) {
                if (recordElement.value() != null && recordElement.value().contains(marker)) {
                    return true;
                }
            }
        }
        return false;
    }
}
