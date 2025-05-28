package com.pm.patientservice.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class KafkaProducer {
    private final KafkaTemplate<String, byte[]> kafkaTemplate; // Definir os tipos da mensagem, e tambem envia as mensagens. Uma chave String com valor de byte[]


}
