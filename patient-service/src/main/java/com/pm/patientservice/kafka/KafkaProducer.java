package com.pm.patientservice.kafka;

import com.pm.patientservice.model.Patient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

@RequiredArgsConstructor
@Service
public class KafkaProducer {
    private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);
    private final KafkaTemplate<String, byte[]> kafkaTemplate; // Definir os tipos da mensagem, e tambem envia as mensagens. Uma chave String com valor de byte[]

    public void sendEvent(Patient patient) {
        PatientEvent event = PatientEvent.newBuilder()
                .setPatientId(patient.getId().toString())
                .setName(patient.getName())
                .setEmail(patient.getEmail())
                .setEventType("PATIENT_CREATED")
                .build();

        try {
            kafkaTemplate.send("patient", event.toByteArray()); // Converter a mensagem pra um byte array para manter o tamanho da mensagem pequeno e também deixa mais fácil converter esse evento para um objeto no código do consumer
        } catch (Exception e) {
            log.error("Error sending PatientCreated event: {}", event);
        }
    }
}
