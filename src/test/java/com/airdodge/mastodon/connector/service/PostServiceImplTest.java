package com.airdodge.mastodon.connector.service;

import com.airdodge.mastodon.connector.client.MastodonClient;
import com.airdodge.mastodon.connector.config.kafka.KafkaTopicConfiguration;
import com.airdodge.mastodon.connector.model.MastodonData;
import com.airdodge.mastodon.connector.model.Post;
import com.airdodge.mastodon.connector.repository.PostReactiveRepository;
import com.airdodge.mastodon.connector.service.impl.PostServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderResult;
import reactor.test.StepVerifier;
import org.springframework.http.codec.ServerSentEvent;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PostServiceImplTest {

    private PostReactiveRepository postReactiveRepository;
    private MastodonClient mastodonClient;
    private PostService postService;
    private KafkaSender<String, MastodonData> kafkaSender;

    @BeforeEach
    void setUp() {
        postReactiveRepository = Mockito.mock(PostReactiveRepository.class);
        mastodonClient = Mockito.mock(MastodonClient.class);
        kafkaSender = Mockito.mock(KafkaSender.class);
        KafkaTopicConfiguration kafkaTopicConfiguration = new KafkaTopicConfiguration();
        KafkaTopicConfiguration.PostTopic postTopic = new KafkaTopicConfiguration.PostTopic();
        postTopic.setName("posts");
        kafkaTopicConfiguration.setPosts(postTopic);
        postService = new PostServiceImpl(postReactiveRepository, mastodonClient, kafkaSender, kafkaTopicConfiguration);
    }

    @Test
    void testSavePost() {
        // given
        String id = "1";
        Long createdAt = 1625152800L;
        Post givenPost = new Post(id, createdAt);

        when(postReactiveRepository.save(any(Post.class))).thenReturn(Mono.just(givenPost));

        // when
        Mono<Post> result = postService.savePost(id, createdAt);

        // then
        StepVerifier.create(result)
                .expectNext(givenPost)
                .verifyComplete();

        verify(postReactiveRepository).save(ArgumentMatchers.argThat(p ->
                p.id().equals(id) && p.createdAt().equals(createdAt)));
    }

    @Test
    void testGetPostById() {
        // given
        String id = "1";
        Long createdAt = 1625152800L;
        Post givenPost = new Post(id, createdAt);

        when(postReactiveRepository.findById(id)).thenReturn(Mono.just(givenPost));

        // when
        Mono<Post> result = postService.getPostById(id);

        // then
        StepVerifier.create(result)
                .expectNext(givenPost)
                .verifyComplete();

        verify(postReactiveRepository).findById(id);
    }

    @Test
    void testGetPostsStream_forNonDeleteEvent() {
        // given
        MastodonData mastodonData = new MastodonData("1", Instant.EPOCH, "content1");


        ServerSentEvent<MastodonData> sse = ServerSentEvent.<MastodonData>builder()
                .event("create")
                .data(mastodonData)
                .build();

        when(postReactiveRepository.save(any(Post.class))).thenReturn(
                Mono.just(new Post(mastodonData.id(), mastodonData.createdAt().getEpochSecond()))
        );
        when(mastodonClient.getPostsSteam()).thenReturn(Flux.just(sse));
        SenderResult<Object> dummySenderResult = Mockito.mock(SenderResult.class);
        when(kafkaSender.send(any())).thenReturn(Flux.just(dummySenderResult));

        // when
        Flux<MastodonData> result = postService.getPostsStream();

        // then
        StepVerifier.create(result)
                .expectNext(mastodonData)
                .verifyComplete();

        verify(postReactiveRepository).save(any(Post.class));
    }

    @Test
    void testGetPostsStream_forDeleteEvent() {
        // given
        MastodonData mastodonData = new MastodonData("1", Instant.EPOCH, "content1");

        ServerSentEvent<MastodonData> sse = ServerSentEvent.<MastodonData>builder()
                .event("delete")
                .data(mastodonData)
                .build();

        when(postReactiveRepository.deleteById(mastodonData.id())).thenReturn(Mono.empty());
        when(mastodonClient.getPostsSteam()).thenReturn(Flux.just(sse));
        SenderResult<Object> dummySenderResult = Mockito.mock(SenderResult.class);
        when(kafkaSender.send(any())).thenReturn(Flux.just(dummySenderResult));

        // when
        Flux<MastodonData> result = postService.getPostsStream();

        // then
        StepVerifier.create(result)
                .expectNext(mastodonData)
                .verifyComplete();

        verify(postReactiveRepository).deleteById(mastodonData.id());
    }
}
