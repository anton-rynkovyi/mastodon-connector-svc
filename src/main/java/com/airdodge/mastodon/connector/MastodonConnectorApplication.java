package com.airdodge.mastodon.connector;

import com.airdodge.mastodon.connector.client.MastodonProperties;
import com.airdodge.mastodon.connector.config.kafka.KafkaTopicConfiguration;
import com.airdodge.mastodon.connector.service.PostService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@Slf4j
@AllArgsConstructor
@SpringBootApplication
@EnableConfigurationProperties({MastodonProperties.class, KafkaTopicConfiguration.class})
public class MastodonConnectorApplication {

    private PostService postService;

    public static void main(String[] args) {
        SpringApplication.run(MastodonConnectorApplication.class, args);
    }

    @Bean
    @Profile("local")
    public ApplicationRunner startupRunner() {
        return args -> postService.getPostsStream()
                .subscribe(
                        data -> log.debug("Received post with id: {} ", data.id()),
                        error -> log.error("Error processing Mastodon stream: {} ", error.getMessage()),
                        () -> log.info("Mastodon stream completed")
                );
    }
}