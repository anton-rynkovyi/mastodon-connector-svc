package com.airdodge.mastodon.connector.service.impl;

import com.airdodge.mastodon.connector.client.MastodonClient;
import com.airdodge.mastodon.connector.config.KafkaTopicConfiguration;
import com.airdodge.mastodon.connector.model.MastodonData;
import com.airdodge.mastodon.connector.model.MastodonSseType;
import com.airdodge.mastodon.connector.model.Post;
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

    private final KafkaSender<String, MastodonData> kafkaSender;

    private final KafkaTopicConfiguration kafkaTopicConfiguration;

    @Override
    public Mono<Post> savePost(String id, Long createdAt) {
        return postReactiveRepository.save(new Post(id, createdAt));
    }

    @Override
    public Mono<Post> getPostById(String id) {
        return postReactiveRepository.findById(id);
    }

    @Override
    public Flux<MastodonData> getPostsStream() {
        return mastodonClient.getPostsSteam()
                .filter(sse ->
                        Objects.nonNull(sse.data()) && Objects.nonNull(sse.data().id()))
                .flatMap(sse ->
                        storeSseEventAndGetMono(sse)
                                .flatMap(this::sendKafkaEvent))
                .mapNotNull(ServerSentEvent::data);
    }

    private Mono<ServerSentEvent<MastodonData>> storeSseEventAndGetMono(ServerSentEvent<MastodonData> sse) {
        return MastodonSseType.fromValue(sse.event()) == MastodonSseType.DELETE
                ? postReactiveRepository.deleteById(sse.data().id()).thenReturn(sse)
                : postReactiveRepository.save(new Post(
                        sse.data().id(),
                        sse.data().createdAt() != null
                                ? sse.data().createdAt().getEpochSecond()
                                : Instant.now().getEpochSecond()))
                .thenReturn(sse);
    }

    private Mono<ServerSentEvent<MastodonData>> sendKafkaEvent(ServerSentEvent<MastodonData> sse) {
        ProducerRecord<String, MastodonData> producerRecord =
                new ProducerRecord<>(kafkaTopicConfiguration.getPosts().getName(), sse.data());
        SenderRecord<String, MastodonData, String> senderRecord =
                SenderRecord.create(producerRecord, sse.data().id());
        return kafkaSender.send(Mono.just(senderRecord))
                .next()
                .thenReturn(sse);
    }
}
