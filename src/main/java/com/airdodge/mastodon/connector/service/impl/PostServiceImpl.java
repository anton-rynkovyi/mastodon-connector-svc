package com.airdodge.mastodon.connector.service.impl;

import com.airdodge.mastodon.connector.client.MastodonClient;
import com.airdodge.mastodon.connector.config.kafka.KafkaTopicConfiguration;
import com.airdodge.mastodon.connector.model.MastodonPost;
import com.airdodge.mastodon.connector.model.PostEntity;
import com.airdodge.mastodon.connector.repository.PostReactiveRepository;
import com.airdodge.mastodon.connector.service.PostService;
import lombok.AllArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

import java.time.Instant;
import java.util.Objects;

@Service
@AllArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostReactiveRepository postReactiveRepository;

    private final MastodonClient mastodonClient;

    private final KafkaSender<String, MastodonPost> kafkaSender;

    private final KafkaTopicConfiguration kafkaTopicConfiguration;

    @Override
    public Mono<PostEntity> savePost(String id, String createdBy, Long createdAt) {
        return postReactiveRepository.save(new PostEntity(id, createdBy, createdAt));
    }

    @Override
    public Mono<PostEntity> getPostById(String id) {
        return postReactiveRepository.findById(id);
    }

    @Override
    public Flux<MastodonPost> getPostsStream() {
        return mastodonClient.getPostsSteam()
                .filter(sse ->
                        Objects.nonNull(sse.data()) && Objects.nonNull(sse.data().id()))
                .flatMap(sse ->
                        storeSseEventAndGetMono(sse)
                                .flatMap(this::sendKafkaEvent))
                .mapNotNull(ServerSentEvent::data);
    }

    private Mono<ServerSentEvent<MastodonPost>> storeSseEventAndGetMono(ServerSentEvent<MastodonPost> sse) {
        return postReactiveRepository.save(new PostEntity(
                        sse.data().id(),
                        sse.data().account().id(),
                        sse.data().createdAt() != null
                                ? sse.data().createdAt().getEpochSecond()
                                : Instant.now().getEpochSecond()))
                .thenReturn(sse);

        // if we need to delete posts
//        return MastodonSseType.fromValue(sse.event()) == MastodonSseType.DELETE
//                ? postReactiveRepository.deleteById(sse.data().id()).thenReturn(sse)
//                : postReactiveRepository.save(new PostEntity(
//                        sse.data().id(),
//                        sse.data().account().id(),
//                        sse.data().createdAt() != null
//                                ? sse.data().createdAt().getEpochSecond()
//                                : Instant.now().getEpochSecond()))
//                .thenReturn(sse);
    }

    private Mono<ServerSentEvent<MastodonPost>> sendKafkaEvent(ServerSentEvent<MastodonPost> sse) {
        ProducerRecord<String, MastodonPost> producerRecord = new ProducerRecord<>(
                kafkaTopicConfiguration.getPosts().getName(),
                sse.data().account() != null ? sse.data().account().id() : "-1",
                sse.data()
        );
        SenderRecord<String, MastodonPost, String> senderRecord =
                SenderRecord.create(producerRecord, sse.data().id());
        return kafkaSender.send(Mono.just(senderRecord))
                .next()
                .thenReturn(sse);
    }
}
