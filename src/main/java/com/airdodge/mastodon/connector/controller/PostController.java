package com.airdodge.mastodon.connector.controller;

import com.airdodge.mastodon.connector.model.MastodonData;
import com.airdodge.mastodon.connector.service.PostService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<MastodonData> streamPosts() {
        return postService.getPostsStream();
    }
}
