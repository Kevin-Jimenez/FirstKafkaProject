# First Kafka Project
This project contains a basic example for implementing Kafka with Spring Boot, and the execution step is simple using Docker.

<p align="right">
<img src="https://img.shields.io/badge/STATUS-%20FINISHED-red">
</p>

<h3 align="center">Technologies and tools used</h3>
<p align="center">
  <a href="https://skillicons.dev">
    <img src="https://skillicons.dev/icons?i=java,spring,postman,docker,kafka" />
  </a>
</p>

## Autor
[<img src="https://avatars.githubusercontent.com/u/64028783?v=4" width=115><br><sub>Kevin Santiago Jimenez</sub>](https://github.com/Kevin-Jimenez) 

## str-producer
It is a service that is responsible for providing a chain through Kafka, those messages can be sent through Postman.

### Kafka Admin Configuration

The `KafkaAdminConfig` class handles the configuration required for your application to manage Kafka topics.

### What does this class do?

1. **Configure the Kafka administration client (`KafkaAdmin`)**
    - Use the properties defined in `application.properties`, especially the property:
      ```properties
        spring.kafka.producer.bootstrap-servers: localhost:9092
      ```
    - With this, the application knows which **Kafka cluster** to connect to.

2. **Create topics automatically**
    - In this example, a topic named `str-topic` is created when the application starts.
    - The topic is defined with:
      - **2 partitions**
      - **1 replica**


3. **Using `@Configuration` and `@Bean`**
   - `@Configuration`: Indicates that this class contains Spring beans.
   - `@Bean KafkaAdmin`: Registers the Kafka administration client.
   - `@Bean KafkaAdmin.NewTopics`: Creates one or more topics on the Kafka broker.

### Example of created topic configuration

```java
@Configuration
public class KafkaAdminConfig {

    private String TOPIC_NAME = "str-topic";

    @Autowired
    private KafkaProperties kafkaProperties;

    @Bean
    public KafkaAdmin kafkaAdmin(){
        var config = new HashMap<String, Object>();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        return new KafkaAdmin(config);
    }

    @Bean
    public KafkaAdmin.NewTopics topics(){
        return new KafkaAdmin.NewTopics(
                TopicBuilder.name(TOPIC_NAME)
                        .partitions(2)
                        .replicas(1)
                        .build()
        );
    }
}
```

### Kafka template example

```java
@Configuration
public class StringProducerFactoryConfig {

   @Autowired
   private KafkaProperties kafkaProperties;

   @Bean
   public ProducerFactory<String, String> producerFactory(){
      var config = new HashMap<String, Object>();
      config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
      config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
      config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
      return new DefaultKafkaProducerFactory<>(config);
   }

   @Bean
   public KafkaTemplate<String, String> kafkaTemplate(){
      return new KafkaTemplate<>(producerFactory());
   }
}
```

### Restcontroller
That class exposes a `REST` endpoint at `/producer` that receives a message via POST and sends it to Kafka through the StringProducerService.
👉 It receives an HTTP message and produces (sends) it to Kafka.
```java
@RestController
@RequestMapping("/producer")
public class StringProducerController {

   @Autowired
   private StringProducerService stringProducerService;

   @PostMapping
   public ResponseEntity<?> sendMessage(@RequestBody String message){
      stringProducerService.sendMessage(message);
      return ResponseEntity.status(HttpStatus.CREATED).build();
   }
}
```

### Service producer
This service uses a **KafkaTemplate** to send messages to the `str-topic`.
👉 It sends a message to Kafka and logs whether it was successful or failed, along with partition and offset details.
```java
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
```

## str-consumer (Consumidor)
It is a service that is responsible for consuming the messages sent by the producer through Kafka and processing them.

### Kafka Consumer Configuration
1. **ConsumerFactory**
   - Connects to the Kafka cluster using `bootstrap-servers`.
   - Uses `StringDeserializer` for both key and value.
   - Provides the base configuration for consumers.

2. **ConcurrentKafkaListenerContainerFactory**
   - Creates listener containers to process Kafka messages concurrently.
   - Attaches a message interceptor before the listener receives them.

3. **RecordInterceptor (`validMessage`)**
   - Intercepts each message before processing.
   - If the message contains the word **"Desde"**, it logs it to the console.
   - All messages are still returned (not filtered).

👉 In short: it configures the Kafka consumer with `String` deserialization, concurrent listeners, and an interceptor to validate and log messages.
```java
@Configuration
public class StringConsumerConfig {
   @Autowired
   private KafkaProperties kafkaProperties;

   @Bean
   public ConsumerFactory<String, String> producerFactory(){
      var config = new HashMap<String, Object>();
      config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
      config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
      config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
      return new DefaultKafkaConsumerFactory<>(config);
   }

   @Bean
   public ConcurrentKafkaListenerContainerFactory<String, String> validMessageContainerFactory(ConsumerFactory<String, String> consumerFactory){
      var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
      factory.setConsumerFactory(consumerFactory);
      factory.setRecordInterceptor(validMessage());
      return factory;
   }

   private RecordInterceptor<String, String> validMessage() {
      return (record, consumer) -> {
         if(record.value().contains("Desde")){
            System.out.println("Contiene la palabra 'Desde'");
            return record;
         }
         return record;
      };
   }
}
```
## Kafka Consumer Listener
The `StringConsumerListener` class defines **methods that listen to Kafka messages** on specific topic partitions:

1. **@KafkaListener**
   - Each method is tied to a **consumer group (groupId)**.
   - Subscribes to the `str-topic`, but only to **specific partitions**.
   - Uses the previously configured `containerFactory` (`validMessageContainerFactory`).

2. **Defined listeners**
   - `listener1`: listens to messages from **partition 0** in `group-1`.
   - `listener2`: listens to messages from **partition 1** in `group-1`.
   - `listener3`: listens to messages from **partition 0**, but in a **different group (`group-2`)**.

3. **Console output**
   - Each listener logs which listener received the message and its content.

👉 In short: this class consumes messages from the `str-topic` in different partitions and groups, showing how multiple listeners can process messages independently.

```java
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
```


## Deploy local application

Watch the next video to up services and run localy

[![Ver video](https://img.shields.io/badge/▶️%20Ver%20Video-blue?style=for-the-badge)](https://drive.google.com/file/d/12Q2FawzYcPej85MQzE1WucArAsNzCvan/view?usp=sharing)

