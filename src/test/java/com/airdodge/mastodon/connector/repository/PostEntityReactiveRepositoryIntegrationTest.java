package com.airdodge.mastodon.connector.repository;

import java.time.Instant;

import com.airdodge.mastodon.connector.model.PostEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.cassandra.DataCassandraTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Testcontainers
@DataCassandraTest
@ExtendWith(SpringExtension.class)
public class PostEntityReactiveRepositoryIntegrationTest {

    @Container
    public static CassandraContainer<?> cassandra = new CassandraContainer<>()
            .withInitScript("schema.cql");

    @DynamicPropertySource
    public static void setCassandraProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cassandra.contact-points", () -> cassandra.getHost());
        registry.add("spring.cassandra.port", () -> cassandra.getFirstMappedPort());
        registry.add("spring.cassandra.keyspace-name", () -> "mastodon_test");
        registry.add("spring.cassandra.schema-action", () -> "create_if_not_exists");
    }

    @Autowired
    private PostReactiveRepository repository;

    @BeforeAll
    public static void setupKeyspace() {

    }

    @Test
    public void testSaveAndFind() {
        // given
        PostEntity givenPostEntity = new PostEntity("1", "2", Instant.now().getEpochSecond());

        Mono<PostEntity> savedMono = repository.save(givenPostEntity);

        StepVerifier.create(savedMono)
                .expectNextMatches(saved ->
                        saved.id().equals(givenPostEntity.id())
                                && saved.createdBy().equals(givenPostEntity.createdBy())
                                && saved.createdAt().equals(givenPostEntity.createdAt()))
                .verifyComplete();

        Mono<PostEntity> retrievedMono = repository.findById(givenPostEntity.id());

        StepVerifier.create(retrievedMono)
                .expectNextMatches(retrieved ->
                        retrieved.id().equals(givenPostEntity.id())
                                && retrieved.createdBy().equals(givenPostEntity.createdBy())
                                && retrieved.createdAt().equals(givenPostEntity.createdAt()))
                .verifyComplete();
    }
}
