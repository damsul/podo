package com.podo.global.storage;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "podo.s3")
public record S3Properties(String bucket, String region) {

}
