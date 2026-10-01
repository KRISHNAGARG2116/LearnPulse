package com.learnpulse.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Slf4j
public class PgVectorStoreConfig {

    public static final int EMBEDDING_DIMENSIONS = 768;

    @Bean
    @Primary
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingClient embeddingClient) {
        log.info("Configuring Spring AI PgVectorStore with {} dimensions, COSINE distance, and HNSW index", EMBEDDING_DIMENSIONS);
        return new PgVectorStore(
                jdbcTemplate,
                embeddingClient,
                EMBEDDING_DIMENSIONS,
                PgVectorStore.PgDistanceType.COSINE_DISTANCE,
                true,
                PgVectorStore.PgIndexType.HNSW
        );
    }
}
