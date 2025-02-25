package com.airdodge.mastodon.connector.service;

import com.airdodge.mastodon.connector.client.MastodonClient;
import com.airdodge.mastodon.connector.config.kafka.KafkaTopicConfiguration;
import com.airdodge.mastodon.connector.model.MastodonAccount;
import com.airdodge.mastodon.connector.model.MastodonPost;
import com.airdodge.mastodon.connector.model.PostEntity;
import com.airdodge.mastodon.connector.repository.PostReactiveRepository;
import com.airdodge.mastodon.connector.service.impl.PostServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

public class PostEntityServiceImplTest {

    private PostReactiveRepository postReactiveRepository;
    private MastodonClient mastodonClient;
    private PostService postService;
    private KafkaSender<String, MastodonPost> kafkaSender;

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
        PostEntity givenPostEntity = new PostEntity("1", "2", 1625152800L);

        when(postReactiveRepository.save(any(PostEntity.class))).thenReturn(Mono.just(givenPostEntity));

        // when
        Mono<PostEntity> result = postService.savePost(givenPostEntity.id(), givenPostEntity.createdBy(), givenPostEntity.createdAt());

        // then
        StepVerifier.create(result)
                .expectNext(givenPostEntity)
                .verifyComplete();

        verify(postReactiveRepository).save(ArgumentMatchers.argThat(p ->
                p.id().equals(givenPostEntity.id())
                        && p.createdBy().equals(givenPostEntity.createdBy())
                        && p.createdAt().equals(givenPostEntity.createdAt())));
    }

    @Test
    void testGetPostById() {
        // given
        PostEntity givenPostEntity = new PostEntity("1", "2", 1625152800L);

        when(postReactiveRepository.findById(givenPostEntity.id())).thenReturn(Mono.just(givenPostEntity));

        // when
        Mono<PostEntity> result = postService.getPostById(givenPostEntity.id());

        // then
        StepVerifier.create(result)
                .expectNext(givenPostEntity)
                .verifyComplete();

        verify(postReactiveRepository).findById(givenPostEntity.id());
    }

    @Test
    void testGetPostsStream_forNonDeleteEvent() {
        // given
        MastodonPost givenMastodonPost = new MastodonPost("1", Instant.EPOCH, "content1",
                new MastodonAccount("2", "username"));


        ServerSentEvent<MastodonPost> sse = ServerSentEvent.<MastodonPost>builder()
                .event("create")
                .data(givenMastodonPost)
                .build();

        when(postReactiveRepository.save(any(PostEntity.class))).thenReturn(
                Mono.just(new PostEntity(givenMastodonPost.id(), givenMastodonPost.account().id(),
                        givenMastodonPost.createdAt().getEpochSecond()))
        );
        when(mastodonClient.getPostsSteam()).thenReturn(Flux.just(sse));
        SenderResult<Object> dummySenderResult = Mockito.mock(SenderResult.class);
        when(kafkaSender.send(any())).thenReturn(Flux.just(dummySenderResult));

        // when
        Flux<MastodonPost> result = postService.getPostsStream();

        // then
        StepVerifier.create(result)
                .expectNext(givenMastodonPost)
                .verifyComplete();

        verify(postReactiveRepository).save(any(PostEntity.class));
    }

    @Test
    @Disabled("This test is disabled if we don't have to remove posts")
    void testGetPostsStream_forDeleteEvent() {
        // given
        MastodonPost givenMastodonPost = new MastodonPost("1", Instant.EPOCH, "content1",
                new MastodonAccount("2", "username"));

        ServerSentEvent<MastodonPost> sse = ServerSentEvent.<MastodonPost>builder()
                .event("delete")
                .data(givenMastodonPost)
                .build();

        when(postReactiveRepository.deleteById(givenMastodonPost.id())).thenReturn(Mono.empty());
        when(mastodonClient.getPostsSteam()).thenReturn(Flux.just(sse));
        SenderResult<Object> dummySenderResult = Mockito.mock(SenderResult.class);
        when(kafkaSender.send(any())).thenReturn(Flux.just(dummySenderResult));

        // when
        Flux<MastodonPost> result = postService.getPostsStream();

        // then
        StepVerifier.create(result)
                .expectNext(givenMastodonPost)
                .verifyComplete();

        verify(postReactiveRepository).deleteById(givenMastodonPost.id());
    }
}
