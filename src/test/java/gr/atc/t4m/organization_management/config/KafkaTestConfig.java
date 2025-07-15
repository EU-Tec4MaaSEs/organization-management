package gr.atc.t4m.organization_management.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;

import gr.atc.t4m.organization_management.dto.EventDTO;

@TestConfiguration
@Profile("test")

public class KafkaTestConfig {
        @Bean
    public KafkaTemplate<String, EventDTO> kafkaTemplate() {
        return Mockito.mock(KafkaTemplate.class);
    }
}
