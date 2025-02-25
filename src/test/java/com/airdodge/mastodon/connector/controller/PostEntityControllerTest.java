package com.airdodge.mastodon.connector.controller;

import com.airdodge.mastodon.connector.model.MastodonAccount;
import com.airdodge.mastodon.connector.model.MastodonPost;
import com.airdodge.mastodon.connector.service.PostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;

import static org.mockito.Mockito.when;

@WebFluxTest(controllers = PostController.class)
public class PostEntityControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private PostService postService;

    @Test
    void streamPosts_returnsSse() {
        MastodonAccount account = new MastodonAccount("2", "username");
        MastodonPost givenResponse1 = new MastodonPost("1", Instant.EPOCH, "content1", account);
        MastodonPost givenResponse2 = new MastodonPost("2", Instant.EPOCH, "content2", account);
        MastodonPost givenResponse3 = new MastodonPost("3", Instant.EPOCH, "content2", account);
        Flux<MastodonPost> givenResponseFlux = Flux.just(givenResponse1, givenResponse2, givenResponse3)
                .delayElements(Duration.ofMillis(100));

        when(postService.getPostsStream()).thenReturn(givenResponseFlux);

        webTestClient.get()
                .uri("/api/v1/posts/stream")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .returnResult(MastodonPost.class)
                .getResponseBody()
                .as(StepVerifier::create)
                .expectNext(givenResponse1, givenResponse2, givenResponse3)
                .thenCancel()
                .verify();
    }
}
