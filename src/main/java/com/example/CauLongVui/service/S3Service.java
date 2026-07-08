package com.example.CauLongVui.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Service để upload/delete ảnh lên AWS S3.
 * URL trả về là CloudFront URL (HTTPS), không dùng S3 URL trực tiếp.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.images-prefix:images}")
    private String imagesPrefix;

    @Value("${app.cdn-url}")
    private String cdnUrl; // CloudFront domain, e.g. https://xxx.cloudfront.net

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

    /**
     * Upload ảnh lên S3 và trả về CloudFront URL công khai.
     *
     * @param file   file ảnh từ multipart request
     * @param folder thư mục con (products / rackets / avatars)
     * @return CloudFront URL của ảnh đã upload
     */
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File ảnh không được để trống");
        }

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "image");
        String ext = "";
        int dotIdx = originalName.lastIndexOf('.');
        if (dotIdx >= 0) {
            ext = originalName.substring(dotIdx).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Chỉ chấp nhận file ảnh: jpg, png, gif, webp");
        }

        // Chuẩn hóa folder name
        String safeFolder = sanitizeFolder(folder);

        // Tạo S3 key: images/products/<uuid>.jpg
        String s3Key = imagesPrefix + "/" + safeFolder + "/" + UUID.randomUUID() + ext;

        String contentType = resolveContentType(ext);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(contentType)
                .build();

        try {
            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("Uploaded image to S3: s3://{}/{}", bucketName, s3Key);
        } catch (S3Exception e) {
            log.error("Failed to upload image to S3: {}", e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Lỗi khi upload ảnh lên S3: " + e.awsErrorDetails().errorMessage(), e);
        }

        // Trả về CloudFront URL
        return cdnUrl + "/" + s3Key;
    }

    /**
     * Xóa ảnh khỏi S3 dựa theo CloudFront URL.
     *
     * @param imageUrl CloudFront URL của ảnh cần xóa
     */
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        try {
            // Extract S3 key từ CloudFront URL
            // URL dạng: https://xxx.cloudfront.net/images/products/uuid.jpg
            String s3Key = extractS3Key(imageUrl);
            if (s3Key == null) {
                log.warn("Cannot extract S3 key from URL: {}", imageUrl);
                return;
            }

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("Deleted image from S3: s3://{}/{}", bucketName, s3Key);
        } catch (S3Exception e) {
            log.error("Failed to delete image from S3: {}", e.awsErrorDetails().errorMessage());
            // Không throw — lỗi xóa ảnh không nên làm hỏng logic chính
        }
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private String sanitizeFolder(String folder) {
        if (folder == null || folder.isBlank()) return "misc";
        return switch (folder.toLowerCase().trim()) {
            case "products" -> "products";
            case "rackets"  -> "rackets";
            case "avatars"  -> "avatars";
            default         -> "misc";
        };
    }

    private String resolveContentType(String ext) {
        return switch (ext) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png"          -> "image/png";
            case ".gif"          -> "image/gif";
            case ".webp"         -> "image/webp";
            default              -> "application/octet-stream";
        };
    }

    /**
     * Trích xuất S3 key từ CloudFront URL.
     * Ví dụ: https://xxx.cloudfront.net/images/products/abc.jpg → images/products/abc.jpg
     */
    private String extractS3Key(String imageUrl) {
        try {
            // Bỏ scheme + host, lấy phần path từ ký tự '/' thứ 3
            java.net.URI uri = java.net.URI.create(imageUrl);
            String path = uri.getPath(); // /images/products/abc.jpg
            if (path.startsWith("/")) {
                path = path.substring(1); // bỏ leading slash
            }
            return path.isBlank() ? null : path;
        } catch (Exception e) {
            return null;
        }
    }
}
