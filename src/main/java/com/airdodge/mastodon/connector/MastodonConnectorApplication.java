package com.airdodge.mastodon.connector;

import com.airdodge.mastodon.connector.client.MastodonProperties;
import com.airdodge.mastodon.connector.config.KafkaTopicConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({MastodonProperties.class, KafkaTopicConfiguration.class})
public class MastodonConnectorApplication {
    public static void main(String[] args) {
        SpringApplication.run(MastodonConnectorApplication.class, args);
    }
}