package hyperledger.besu.java.rest.client.config;

import static hyperledger.besu.java.rest.client.config.KafkaProperties.KAFKA_SASL_JASS_ENDPOINT_KEY;
import static hyperledger.besu.java.rest.client.config.KafkaProperties.KAFKA_SASL_MECHANISM_KEY;
import static hyperledger.besu.java.rest.client.config.KafkaProperties.KAFKA_SASL_MECHANISM_VALUE;
import static hyperledger.besu.java.rest.client.config.KafkaProperties.KAFKA_SECURITY_PROTOCOL_KEY;
import static hyperledger.besu.java.rest.client.config.KafkaProperties.KAFKA_SECURITY_PROTOCOL_VALUE;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.maven.surefire.shade.org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/** This class is the configuration class for sending to Chaincode event to eventHub/Kafka Topic. */
@Slf4j
@Configuration
@ConditionalOnProperty("kafka.event-listener.brokerHost")
@RefreshScope
public class KafkaProducerConfig {

  @Autowired private KafkaProperties kafkaProperties;

  public ProducerFactory<String, String> eventProducerFactory(
      KafkaProperties.Producer kafkaProducerProperties) {
    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProducerProperties.getBrokerHost());
    props.put(
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
        org.apache.kafka.common.serialization.StringSerializer.class);
    props.put(
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
        org.apache.kafka.common.serialization.StringSerializer.class);
    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, Boolean.FALSE);
    // Azure event-hub config
    if (StringUtils.isNotEmpty(kafkaProducerProperties.getSaslJaasConfig())) {
      props.put(KAFKA_SECURITY_PROTOCOL_KEY, KAFKA_SECURITY_PROTOCOL_VALUE);
      props.put(KAFKA_SASL_MECHANISM_KEY, KAFKA_SASL_MECHANISM_VALUE);
      props.put(KAFKA_SASL_JASS_ENDPOINT_KEY, kafkaProducerProperties.getSaslJaasConfig());
    }

    // Adding SSL configuration if Kafka Cluster is SSL secured
    if (kafkaProducerProperties.isSslAuthRequired()) {

      SSLAuthFilesCreationHelper.createSSLAuthFiles(kafkaProducerProperties);

      props.put(
          CommonClientConfigs.SECURITY_PROTOCOL_CONFIG,
          kafkaProducerProperties.getSecurityProtocol());
      props.put(
          SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG,
          kafkaProducerProperties.getSslKeystoreLocation());
      props.put(
          SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG,
          kafkaProducerProperties.getSslKeystorePassword());
      props.put(
          SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,
          kafkaProducerProperties.getSslTruststoreLocation());
      props.put(
          SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG,
          kafkaProducerProperties.getSslTruststorePassword());
      props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, kafkaProducerProperties.getSslKeyPassword());
    }

    log.info("Created kafka producer factory");
    return new DefaultKafkaProducerFactory<>(props);
  }

  @Bean
  @RefreshScope
  public KafkaTemplate<String, String> kafkaTemplate() {
    return new KafkaTemplate<>(eventProducerFactory(kafkaProperties.getEventListener()));
  }
}
