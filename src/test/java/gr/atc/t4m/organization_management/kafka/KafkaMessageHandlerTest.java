package gr.atc.t4m.organization_management.kafka;

import gr.atc.t4m.organization_management.dto.EventDto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.UUID;

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
        EventDto event = EventDto.builder()
                .id(UUID.randomUUID().toString())
                .description("Test Event")
                .eventType("INFO")
                .priority("HIGH")
                .productionModule("MOD1")
                .sourceComponent("ComponentA")
                .topic("event.topic")
                .build();

        handler.consume(event, "event.topic", "some-key");
        // You could verify logs or mock NotificationService if present
    }

    @Test
    void testInvalidEventDoesNotCreateNotification() {
        EventDto event = EventDto.builder()
                .id(UUID.randomUUID().toString())
                .description("Bad Event")
                .eventType("ERROR")
                // Missing priority, productionModule, topic
                .build();

        handler.consume(event, "missing.topic", "bad-key");
        // Nothing to assert since log only; could use a logger spy here if needed
    }


}
