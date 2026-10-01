package com.fxzt.vidora.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3Config {
    @Value("${minio.endpoint}") private String endpoint;
    @Value("${minio.accessKey}") private String accessKey;
    @Value("${minio.secretKey}") private String secretKey;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1) // MinIO 不校验，但 SDK 必填
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
//                .forcePathStyle(true) // ⚠️ 必须！否则会变成 virtual-hosted-style 导致 404
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)      // 等价于原来的 forcePathStyle(true)
                        .multiRegionEnabled(false)         // ⚠️ 关键：阻止读 ~/.aws/config
                        .useArnRegionEnabled(false)        // ⚠️ 关键：阻止读 ~/.aws/credentials
                        .build())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)   // ⚠️ 补上这一句
                        .multiRegionEnabled(false)         // ⚠️ 补上
                        .useArnRegionEnabled(false)        // ⚠️ 补上
                        .build())
                .build();
    }
}