package com.planify.user_service.event;

import com.planify.user_service.model.event.InvitationEvent;
import com.planify.user_service.model.event.JoinRequestEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class KafkaProducer {

    private final Environment environment;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishJoinRequestEvent(JoinRequestEvent event) {
        String joinRequestsTopic = environment.getProperty("planify.kafka.topic.join-requests");
        if (joinRequestsTopic != null) {
            kafkaTemplate.send(joinRequestsTopic,
                    event.joinRequestId().toString(),
                    event);
        }
    }

    public void publishInvitationEvent(InvitationEvent event) {
        String invitationsTopic = environment.getProperty("planify.kafka.topic.invitations");
        if (invitationsTopic != null) {
            kafkaTemplate.send(invitationsTopic,
                    event.invitationId().toString(),
                    event);
        }
    }
}
