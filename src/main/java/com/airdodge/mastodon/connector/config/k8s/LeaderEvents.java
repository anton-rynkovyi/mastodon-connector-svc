package com.airdodge.mastodon.connector.config.k8s;

import com.airdodge.mastodon.connector.service.PostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.integration.leader.event.OnGrantedEvent;
import org.springframework.integration.leader.event.OnRevokedEvent;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;


@Slf4j
@Component
@ConditionalOnProperty(prefix = "spring.cloud.kubernetes.leader", name = "enabled", havingValue = "true")
public class LeaderEvents {

    private final PostService postService;

    private Disposable mastodonStreamSubscription;

    public LeaderEvents(PostService postService) {
        this.postService = postService;
    }

    @EventListener
    public void grantLeaderEvent(OnGrantedEvent event) {
        log.info("I'm currently leader!");
        mastodonStreamSubscription = postService.getPostsStream()
                .subscribe(
                        data -> log.debug("Received post with id: {} ", data.id()),
                        error -> log.error("Error processing Mastodon stream: {} ", error.getMessage()),
                        () -> log.info("Mastodon stream completed")
                );
    }

    @EventListener
    public void revokeLeaderEvent(OnRevokedEvent event) {
        log.info("I'm NOT leader anymore!");
        if (mastodonStreamSubscription != null && !mastodonStreamSubscription.isDisposed()) {
            mastodonStreamSubscription.dispose();
        }
    }
}