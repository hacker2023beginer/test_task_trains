package org.vladproj.zhddrop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka")
public record KafkaTicketProperties(boolean enabled, String topic) {

    private static final String DEFAULT_TOPIC = "ticket-confirmed";

    public KafkaTicketProperties {
        if (topic == null || topic.isBlank()) {
            topic = DEFAULT_TOPIC;
        }
    }
}
