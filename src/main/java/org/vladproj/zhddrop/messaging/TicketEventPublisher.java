package org.vladproj.zhddrop.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.vladproj.zhddrop.config.KafkaTicketProperties;
import org.vladproj.zhddrop.event.TicketConfirmedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTicketProperties properties;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketConfirmed(TicketConfirmedEvent event) {
        if (!properties.enabled()) {
            log.debug("Kafka publishing disabled, skipping event for reservation {}", event.reservationId());
            return;
        }
        try {
            kafkaTemplate
                .send(properties.topic(), String.valueOf(event.reservationId()), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Failed to publish TicketConfirmed for reservation {}",
                            event.reservationId(), error);
                    } else if (result != null) {
                        log.debug("TicketConfirmed for reservation {} sent to {}-{}",
                            event.reservationId(),
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().offset());
                    }
                });
        } catch (RuntimeException ex) {
            log.error("Failed to send TicketConfirmed for reservation {}", event.reservationId(), ex);
        }
    }
}
