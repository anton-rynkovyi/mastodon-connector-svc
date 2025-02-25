package com.airdodge.mastodon.connector.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("airdodge.kafka.topic")
public class KafkaTopicConfiguration {

    private PostTopic posts;

    @Getter
    @Setter
    public static class PostTopic {
        private String name;
        private int partitions;
        private short replicationFactor;
    }
}
