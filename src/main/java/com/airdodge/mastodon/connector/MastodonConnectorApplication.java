package com.airdodge.mastodon.connector;

import com.airdodge.mastodon.connector.client.MastodonProperties;
import com.airdodge.mastodon.connector.config.kafka.KafkaTopicConfiguration;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Slf4j
@AllArgsConstructor
@SpringBootApplication
@EnableConfigurationProperties({MastodonProperties.class, KafkaTopicConfiguration.class})
public class MastodonConnectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MastodonConnectorApplication.class, args);
    }

}