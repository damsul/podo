package com.podo.global.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

    @Bean
    public S3Client s3Client(S3Properties props) {
        // 자격증명은 기본 체인(EC2 역할 / 환경변수 / ~/.aws)이 자동으로 찾음
        return S3Client.builder().region(Region.of(props.region())).build();
    }
}
