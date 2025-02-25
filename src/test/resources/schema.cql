CREATE KEYSPACE IF NOT EXISTS mastodon_test
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1 };