package com.planify.user_service.event;

import com.planify.user_service.model.event.InvitationRespondedEvent;
import com.planify.user_service.model.event.InvitationSentEvent;
import com.planify.user_service.model.event.JoinRequestRespondedEvent;
import com.planify.user_service.model.event.JoinRequestsSentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaProducer {

    private final Environment environment;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishJoinRequestSentEvent(JoinRequestsSentEvent event) {
        String joinRequestsTopic = environment.getProperty("planify.kafka.topic.join-request-sent");
        if (joinRequestsTopic != null) {
            kafkaTemplate.send(joinRequestsTopic,
                    event.joinRequestId().toString(),
                    event);
        }
    }

    public void publishInvitationSentEvent(InvitationSentEvent event) {
        String invitationsTopic = environment.getProperty("planify.kafka.topic.invitation-sent");
        if (invitationsTopic != null) {
            kafkaTemplate.send(invitationsTopic,
                    event.invitationId().toString(),
                    event);
        }
    }

    public void publishJoinRequestRespondedEvent(JoinRequestRespondedEvent event) {
        String joinRequestsTopic = environment.getProperty("planify.kafka.topic.join-request-responded");
        if (joinRequestsTopic != null) {
            kafkaTemplate.send(joinRequestsTopic,
                    event.joinRequestId().toString(),
                    event);
        }
    }

    public void publishInvitationRespondedEvent(InvitationRespondedEvent event) {
        String invitationsTopic = environment.getProperty("planify.kafka.topic.invitation-responded");
        if (invitationsTopic != null) {
            kafkaTemplate.send(invitationsTopic,
                    event.invitationId().toString(),
                    event);
        }
    }
}
