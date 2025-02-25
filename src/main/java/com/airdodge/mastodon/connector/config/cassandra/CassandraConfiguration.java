package com.airdodge.mastodon.connector.config.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.cassandra.CassandraProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CassandraConfiguration {

    private static final String CREATE_KEYSPACE_CQL = """
            CREATE KEYSPACE IF NOT EXISTS %s
              WITH replication = {'class':'SimpleStrategy','replication_factor':'1'}
            """
            ;

    @Bean
    public CommandLineRunner createKeyspace(CqlSession session, CassandraProperties cassandraProperties) {
        return args -> session.execute(CREATE_KEYSPACE_CQL.formatted(cassandraProperties.getKeyspaceName()));
    }
}
