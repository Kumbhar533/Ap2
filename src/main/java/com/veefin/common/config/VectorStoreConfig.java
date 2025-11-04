package com.veefin.common.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class VectorStoreConfig {

    @Value("${spring.ai.vectorstore.qdrant.host}")
    private String host;

    @Value("${spring.ai.vectorstore.qdrant.port}")
    private int port;

    @Value("${spring.ai.vectorstore.qdrant.collection-name}")
    private String collectionName;

    @Value("${spring.ai.vectorstore.qdrant.embedding-model}")
    private String embeddingModel;




//    // 2️⃣ Embedding model
//    @Bean
//    public EmbeddingModel embeddingModel(OllamaApi ollamaApi) {
//        return OllamaEmbeddingModel.builder()
//                .ollamaApi(ollamaApi)
//                .defaultOptions(OllamaOptions.fromOptions(
//                        OllamaOptions.builder()
//                                .model("nomic-embed-text")
//                                .build()))
//                .build();
//    }


    @Bean
    public EmbeddingModel embeddingModel(OpenAiApi openAiApi) {
        // 1. Use the most modern options class (OpenAiEmbeddingOption)
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(embeddingModel)
                .build();
        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, options);
    }

    @Bean
    public QdrantClient qdrantClient() {
        return new QdrantClient(
                QdrantGrpcClient.newBuilder(host, port, false).build()
        );
    }

    // 4️⃣ Vector store
    @Bean
    public VectorStore qdrantVectorStore(QdrantClient qdrantClient, EmbeddingModel embeddingModel) {
        return QdrantVectorStore.builder(qdrantClient, embeddingModel)
                .collectionName(collectionName)
                .initializeSchema(true)
                .build();
    }
}
