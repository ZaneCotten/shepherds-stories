package com.shepherdsstories.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Service
public class S3Service {

    private final String bucketName;
    private final S3Presigner resigned;
    private final S3Client s3Client;

    public S3Service(@Value("${aws.s3.bucket}") String bucketName,
                     @Value("${aws.region}") String region,
                     @Value("${aws.access.key}") String accessKey,
                     @Value("${aws.secret.key}") String secretKey) {
        this.bucketName = bucketName;
        this.resigned = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    public String generatePresignedUrl(String key) {
        if (key == null) return null;

        GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(60))
                .getObjectRequest(b -> b.bucket(bucketName).key(key))
                .build();

        PresignedGetObjectRequest presignedGetObjectRequest = resigned.presignGetObject(getObjectPresignRequest);
        return presignedGetObjectRequest.url().toString();
    }

    public String generateUploadUrl(String key, String contentType) {
        PutObjectPresignRequest putObjectPresignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(b -> b.bucket(bucketName).key(key).contentType(contentType))
                .build();

        PresignedPutObjectRequest presignedPutObjectRequest = resigned.presignPutObject(putObjectPresignRequest);
        return presignedPutObjectRequest.url().toString();
    }

    public String getBucketName() {
        return bucketName;
    }

    public void deleteObject(String key) {
        if (key == null) return;
        s3Client.deleteObject(b -> b.bucket(bucketName).key(key));
    }
}
