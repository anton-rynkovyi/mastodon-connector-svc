package com.airdodge.mastodon.connector.config;

import com.airdodge.mastodon.connector.service.PostService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration
@Profile("local")
@AllArgsConstructor
public class LocalPostsStreamingConfiguration {

    private final PostService postService;

    @Bean
    public ApplicationRunner startupRunner() {
        return args -> postService.getPostsStream()
                .subscribe(
                        data -> log.debug("Received post with id: {} ", data.id()),
                        error -> log.error("Error processing Mastodon stream: {} ", error.getMessage()),
                        () -> log.info("Mastodon stream completed")
                );
    }
}
