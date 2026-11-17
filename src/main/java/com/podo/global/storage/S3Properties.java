package com.podo.global.storage;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "podo.s3")
public record S3Properties(String bucket, String region, String prefix) {

    public S3Properties {
        if (prefix == null || prefix.isBlank()) {
            prefix = "profiles/";
        }
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";   // "dev/profiles"처럼 써도 "/"를 붙여 줌
        }
    }
}

