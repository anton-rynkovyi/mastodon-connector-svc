package com.airdodge.mastodon.connector.service;

import com.airdodge.mastodon.connector.model.MastodonData;
import com.airdodge.mastodon.connector.model.Post;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PostService {

    Mono<Post> savePost(String id, Long createdAt);

    Mono<Post> getPostById(String id);

    Flux<MastodonData> getPostsStream();
}
