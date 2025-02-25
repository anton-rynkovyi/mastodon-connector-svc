package com.airdodge.mastodon.connector.client;

import com.airdodge.mastodon.connector.model.MastodonData;
import com.airdodge.mastodon.connector.model.MastodonSseType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
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

    private final WebClient webClient;

    public MastodonClient(MastodonProperties mastodonProperties) {
        this.mastodonProperties = mastodonProperties;
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create(ConnectionProvider.builder(CONNECTION_POOL_NAME)
                                        .maxConnections(300)
                                        .maxIdleTime(Duration.ofSeconds(100))
                                        .build())
                                .followRedirect(true))
                ).build();
    }

    public Flux<ServerSentEvent<MastodonData>> getPostsSteam() {
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
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<MastodonData>>() {
                })
                .mapNotNull(sse -> {
                            MastodonSseType mastodonSseType = MastodonSseType.fromValue(sse.event());
                            ServerSentEvent.Builder<MastodonData> sseBuilder = ServerSentEvent.<MastodonData>builder()
                                    .event(sse.event() != null ? sse.event() : MastodonSseType.UNKNOWN.name());
                            return mastodonSseType == MastodonSseType.DELETE
                                    ? sseBuilder
                                    .data(new MastodonData(String.valueOf(sse.data()), null, null))
                                    .build()
                                    : sseBuilder
                                    .data(sse.data())
                                    .build();
                        }
                )
                .doOnError(it -> log.warn("Error streaming data"))
                .retryWhen(Retry.fixedDelay(10, Duration.ofSeconds(1)))
                .log();
    }
}
