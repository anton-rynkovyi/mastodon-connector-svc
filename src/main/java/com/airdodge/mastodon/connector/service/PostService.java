package com.airdodge.mastodon.connector.service;

import com.airdodge.mastodon.connector.model.MastodonPost;
import com.airdodge.mastodon.connector.model.PostEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PostService {

    Mono<PostEntity> savePost(String id, String createdBy, Long createdAt);

    Mono<PostEntity> getPostById(String id);

    Flux<MastodonPost> getPostsStream();
}
