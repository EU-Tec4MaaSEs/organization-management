package gr.atc.t4m.organization_management.kafka;

import gr.atc.t4m.organization_management.dto.EventDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaAdmin;


import static org.mockito.Mockito.*;

class KafkaMessageHandlerTest {

    private KafkaAdmin kafkaAdmin;
    private KafkaMessageHandler handler;

    @BeforeEach
    void setup() {
        kafkaAdmin = mock(KafkaAdmin.class);
        handler = new KafkaMessageHandler(kafkaAdmin);
    }

    @Test
    void testValidEventCreatesNotification() {
        EventDTO event = EventDTO.builder()
                .description("Test Event")
                .eventType("INFO")
                .priority("HIGH")
                .sourceComponent("ComponentA")
                .build();

        handler.consume(event, "event.topic", "some-key");
        // You could verify logs or mock NotificationService if present
    }

    @Test
    void testInvalidEventDoesNotCreateNotification() {
        EventDTO event = EventDTO.builder()
                .description("Bad Event")
                .eventType("ERROR")
                // Missing priority, productionModule, topic
                .build();

        handler.consume(event, "missing.topic", "bad-key");
        // Nothing to assert since log only; could use a logger spy here if needed
    }


}
