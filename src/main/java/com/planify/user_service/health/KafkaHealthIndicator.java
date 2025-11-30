package com.planify.user_service.health;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public Health health() {
        try {
            // Pingamo Kafko tako da pošljemo metadata zahtevek
            kafkaTemplate.partitionsFor("health-check-topic");

            return Health.up().withDetail("kafka", "Available").build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("kafka", "Unavailable")
                    .withException(e)
                    .build();
        }
    }
}
