package com.ksjimen.kafka.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class StringProducerService {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void sendMessage(String message){
        kafkaTemplate.send("str-topic", message).whenComplete((result, ex) ->{
            if(ex != null){
                System.out.println("Error la enviar el mensaje: "+ex.getMessage());
            }
            System.out.println("Mensaje enviado con exito: "+result.getProducerRecord().value());
            System.out.println("Particion: "+result.getRecordMetadata().partition()+", offset: "+result.getRecordMetadata().offset());
        });
    }
}
