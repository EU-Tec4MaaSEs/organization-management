package gr.atc.t4m.organization_management.kafka;


import java.time.LocalDateTime;
import java.util.List;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;



import gr.atc.t4m.organization_management.dto.EventDto;
import gr.atc.t4m.organization_management.dto.NotificationDto;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class KafkaMessageHandler {

    @Value("${kafka.topics}")
    @SuppressWarnings("unused")
    private List<String> kafkaTopics;

    @Value("${use-case.pilot}")
    private String pilot;

    private final KafkaAdmin kafkaAdmin;


    public KafkaMessageHandler(KafkaAdmin kafkaAdmin) {
        kafkaAdmin.setAutoCreate(true);
        this.kafkaAdmin = kafkaAdmin;

    }

    /**
     * Kafka consumer method to receive a JSON Event message - From Kafka Producers
     *
     * @param event: Event occurred in T4M
     */
    @KafkaListener(topics = "#{'${kafka.topics}'.split(',')}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(EventDto event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic, @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String messageKey){
        // Validate that same essential variables are present
        if (!isValidEvent(event)){
            log.error("Kafka message error - Either priority or production module or Topic are missing from the event. Message is discarded! Data: {}",event);
            return;
        }

        log.info("Event Description: {}", event.getDescription());
        log.info("Event Type: {}", event.getEventType());

        // Create and store the notification
        NotificationDto eventNotification = generateNotificationFromEvent(event);
        log.info("Notification created: {}", eventNotification);
             

    }

   /*
     * Helper method to generate a Notification from Event
     */
    private NotificationDto generateNotificationFromEvent(EventDto event){
        return  NotificationDto.builder()
                .sourceComponent(event.getSourceComponent())
                .productionModule(event.getProductionModule())
                .relatedEvent(event.getId())
                .relatedAssignment(null)
                .timestamp(LocalDateTime.now().withNano(0))
                .priority(event.getPriority())
                .description(event.getDescription())
                .build();
    }
    private boolean isValidEvent(EventDto event) {
        return event.getPriority() != null &&
                event.getProductionModule() != null &&
                event.getTopic() != null;
    }

    /**
     * Dynamic creation of new topics in Kafka with Kafka Admin
     *
     * @param topicName : String value of newly created topic
     * @param partitions : Number of partitions for the specified topic
     * @param replicas : Number of replicas for the specified topic
     */
    public void createTopic(String topicName, int partitions, int replicas) {
        NewTopic topic = TopicBuilder.name(topicName)
                .partitions(partitions)
                .replicas(replicas)
                .build();

        kafkaAdmin.createOrModifyTopics(topic);
        log.info("Topic created: {}", topicName);
    }
}
