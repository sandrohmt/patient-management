package com.pm.analyticsservice.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

@Service
public class KafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

    @KafkaListener(topics = "patient", groupId = "analytics-service") // Conecta o kafka consumer com o kafka topic, o trabalho é feito por debaixo dos planos por essa anotação KafkaListener. GroupId serve para o broker conseguir administrar diferentes consumers
    public void consumeEvent(byte[] event) { // Qualquer evento enviado para o tópico patient será consumido ou será lido por esse método
        try {
            PatientEvent patientEvent = PatientEvent.parseFrom(event);
            // Lógica de negócios relacionados a analytics aqui.

            log.info("received Patient Event: [PatientId={}, PatientName={}, PatientEmail={}"
                    , patientEvent.getPatientId(), patientEvent.getName(), patientEvent.getEmail());
        } catch (InvalidProtocolBufferException e) {
            log.error("Error deserializing event {}", e.getMessage()); // Não precisa throw a exception, porque isso faria o analytics-service inteiro cair, apenas uma mensagem avisando já é o suficiente
        }
    }
}
