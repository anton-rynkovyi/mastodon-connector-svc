package com.airdodge.mastodon.connector.repository;

import com.airdodge.mastodon.connector.model.Post;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostReactiveRepository extends ReactiveCassandraRepository<Post, String> {
}
