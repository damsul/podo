package com.podo.global.storage;

import com.podo.global.exception.ImageUploadException;
import com.podo.global.exception.InvalidImageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileImageStorage {

    private static final long MAX_SIZE = 5L * 1024 * 1024;

    private final S3Client s3Client;
    private final S3Properties props;

    public String upload(Long userId, MultipartFile file) {
        if (file.getSize() > MAX_SIZE) {
            throw new InvalidImageException();
        }
        byte [] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ImageUploadException();
        }
        ImageType type = detect(bytes);

        String key = "profiles/%d/%s.%s".formatted(userId, UUID.randomUUID(), type.ext);
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(props.bucket())
                            .key(key)
                            .contentType(type.mime)
                            .cacheControl("public, max-age=31536000")
                            .build(),
                    RequestBody.fromBytes(bytes));
        } catch (SdkException e) {
            log.error("S3 업로드 실패 key={}", key, e);
            throw new ImageUploadException();
        }
        return baseUrl() + key;
    }

    // 우리 버킷의 profiles/ 아래 파일만 삭제 (소셜 로그인 프로필 URL 등은 건드리지 않음)
    public void deleteIfOwned(String url) {
        if (url == null || !url.startsWith(baseUrl() + "profiles/")) {
            return;
        }
        String key = url.substring(baseUrl().length());
        try {
            s3Client.deleteObject(b -> b.bucket(props.bucket()).key(key));
        } catch (SdkException e) {
            log.warn("S3 삭제 실패 key = {}", key, e); // 삭제 실패로 사용자 요청까지 실패시키진 않음
        }
    }

    private String baseUrl() {
        return "https://%s.s3.%s.amazonaws.com/".formatted(props.bucket(), props.region());
    }

    private ImageType detect(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return ImageType.JPEG;
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return ImageType.PNG;
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return ImageType.WEBP;
        throw new InvalidImageException();
    }

    private enum ImageType {
        JPEG("image/jpeg", "jpg"),
        PNG("image/png", "png"),
        WEBP("image/webp", "webp");

        final String mime;
        final String ext;

        ImageType(String mime, String ext) {
            this.mime = mime;
            this.ext = ext;
        }
    }
}
