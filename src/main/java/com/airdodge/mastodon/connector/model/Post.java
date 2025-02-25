package com.airdodge.mastodon.connector.model;


import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table("posts")
public record Post(
        @PrimaryKey
        String id,

        @Column("created_at")
        Long createdAt
) {
}
