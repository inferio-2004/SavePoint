package com.example.Savepoint.Game.Search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface GameSearchRepository extends ElasticsearchRepository<GameDocument, String> {
}