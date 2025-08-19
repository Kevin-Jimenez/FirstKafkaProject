package com.ksjimen.kafka.listeners;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.stereotype.Component;

@Component
public class StringConsumerListener {

    @KafkaListener(groupId = "group-1",
            topicPartitions = @TopicPartition(topic = "str-topic", partitions = {"0"}),
            containerFactory = "validMessageContainerFactory")
    public void listener1(String message){
        System.out.println("LISTENER1 ::: Recibiendo mensaje: "+message);
    }

    @KafkaListener(groupId = "group-1",
            topicPartitions = @TopicPartition(topic = "str-topic", partitions = {"1"}),
            containerFactory = "validMessageContainerFactory")
    public void listener2(String message){
        System.out.println("LISTENER2 ::: Recibiendo mensaje: "+message);
    }

    @KafkaListener(groupId = "group-2",
            topicPartitions = @TopicPartition(topic = "str-topic", partitions = {"0"}),
            containerFactory = "validMessageContainerFactory")
    public void listener3(String message){
        System.out.println("LISTENER3 ::: Recibiendo mensaje: "+message);
    }

}
