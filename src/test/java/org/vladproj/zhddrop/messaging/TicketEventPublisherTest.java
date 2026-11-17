package org.vladproj.zhddrop.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.vladproj.zhddrop.config.KafkaTicketProperties;
import org.vladproj.zhddrop.entity.RateCode;
import org.vladproj.zhddrop.event.TicketConfirmedEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void sendsEventToConfiguredTopic() {
        TicketEventPublisher publisher = new TicketEventPublisher(
            kafkaTemplate, new KafkaTicketProperties(true, "ticket-confirmed"));

        TicketConfirmedEvent event = event();

        when(kafkaTemplate.send(eq("ticket-confirmed"), eq("7"), any()))
            .thenReturn(CompletableFuture.completedFuture(null));

        publisher.onTicketConfirmed(event);

        verify(kafkaTemplate).send("ticket-confirmed", "7", event);
    }

    @Test
    void doesNothingWhenPublishingDisabled() {
        TicketEventPublisher publisher = new TicketEventPublisher(
            kafkaTemplate, new KafkaTicketProperties(false, "ticket-confirmed"));

        publisher.onTicketConfirmed(event());

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void doesNotFailWhenBrokerIsUnavailable() {
        TicketEventPublisher publisher = new TicketEventPublisher(
            kafkaTemplate, new KafkaTicketProperties(true, "ticket-confirmed"));

        when(kafkaTemplate.send(anyString(), anyString(), any()))
            .thenThrow(new org.springframework.kafka.KafkaException("broker is down"));

        publisher.onTicketConfirmed(event());
    }

    private static TicketConfirmedEvent event() {
        return new TicketConfirmedEvent(
            7, 101, "002A", RateCode.PROMO, 2, new BigDecimal("3000.00"), Instant.now());
    }
}
