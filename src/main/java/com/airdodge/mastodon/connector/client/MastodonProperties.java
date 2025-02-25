package com.airdodge.mastodon.connector.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("airdodge.mastodon-connector-svc.mastodon")
public class MastodonProperties {

    private String accessToken;

    private String url;

    private Api api;

    @Getter
    @Setter
    public static class Api {
        private String getPostsStreaming;
    }
}
