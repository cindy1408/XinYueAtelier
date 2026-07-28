package com.xinyue.atelier.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThat;

class S3ConfigTest {

    private S3Config s3Config;

    @BeforeEach
    void setUp() {
        s3Config = new S3Config();
        ReflectionTestUtils.setField(s3Config, "accessKeyId", "test-access-key");
        ReflectionTestUtils.setField(s3Config, "secretAccessKey", "test-secret-key");
        ReflectionTestUtils.setField(s3Config, "region", "eu-west-2");
    }

    @Test
    void s3ClientBeanIsCreatedSuccessfully() {
        S3Client client = s3Config.s3Client();

        assertThat(client).isNotNull();
        client.close();
    }

    @Test
    void s3PresignerBeanIsCreatedSuccessfully() {
        S3Presigner presigner = s3Config.s3Presigner();

        assertThat(presigner).isNotNull();
        presigner.close();
    }
}