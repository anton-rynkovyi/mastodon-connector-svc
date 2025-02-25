package com.airdodge.mastodon.connector.client;

import com.airdodge.mastodon.connector.model.MastodonPost;
import com.airdodge.mastodon.connector.model.MastodonSseType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Component
public class MastodonClient {

    private static final String CONNECTION_POOL_NAME = "mastodon-connection-pool";

    private static final String PATH_ACCESS_TOKEN = "access_token";

    private final MastodonProperties mastodonProperties;

    private final ObjectMapper objectMapper;

    private final WebClient webClient;

    public MastodonClient(MastodonProperties mastodonProperties, ObjectMapper objectMapper) {
        this.mastodonProperties = mastodonProperties;
        this.objectMapper = objectMapper;
        HttpClient httpClient = HttpClient.create(ConnectionProvider.builder(CONNECTION_POOL_NAME)
                        .maxConnections(50)
                        .maxIdleTime(Duration.ofSeconds(45))
                        .build())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .responseTimeout(Duration.ofSeconds(30))
                .followRedirect(true);
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    public Flux<ServerSentEvent<MastodonPost>> getPostsSteam() {
        return webClient.get()
                .uri(uriBuilder -> UriComponentsBuilder
                        .fromUriString(mastodonProperties.getUrl())
                        .path(mastodonProperties.getApi().getGetPostsStreaming())
                        .queryParam(PATH_ACCESS_TOKEN, mastodonProperties.getAccessToken())
                        .build(true)
                        .toUri()
                )
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
//              Mastodon doesn't send body when 'delete'!? Just sends id as a number!
//                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<MastodonData>>() {})
                .bodyToFlux(ServerSentEvent.class)
                .filter(sse -> sse.event() != null
                        && MastodonSseType.fromValue(sse.event()) != MastodonSseType.DELETE)
                .mapNotNull(this::parseAndGetSseMastodonData)
                .doOnError(it -> log.error("Error streaming data"))
                .retryWhen(Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(1))
                        .filter(throwable -> throwable instanceof java.net.SocketException))
                .log();
    }

    private ServerSentEvent<MastodonPost> parseAndGetSseMastodonData(ServerSentEvent<?> sse) {
        MastodonSseType mastodonSseType = MastodonSseType.fromValue(sse.event());
        ServerSentEvent.Builder<MastodonPost> sseBuilder = ServerSentEvent.<MastodonPost>builder()
                .event(sse.event() != null ? sse.event() : MastodonSseType.UNKNOWN.name());

        return sseBuilder
                .data(objectMapper.convertValue(sse.data(), MastodonPost.class))
                .build();

        // needed if we're gonna delete posts
//        return mastodonSseType == MastodonSseType.DELETE
//                ? sseBuilder
//                .data(new MastodonPost(String.valueOf(sse.data()), null, null))
//                .build()
//                : sseBuilder
//                .data(objectMapper.convertValue(sse.data(), MastodonPost.class))
//                .build();
    }
}
