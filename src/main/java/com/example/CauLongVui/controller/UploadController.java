package com.example.CauLongVui.controller;

import com.example.CauLongVui.dto.ApiResponse;
import com.example.CauLongVui.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Controller upload ảnh lên AWS S3.
 * Endpoint: POST /api/upload?folder=products|rackets|avatars
 * Trả về CloudFront URL của ảnh đã upload.
 */
@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final S3Service s3Service;

    /**
     * Upload file ảnh lên S3.
     *
     * @param file   file ảnh (multipart/form-data)
     * @param folder thư mục đích: products | rackets | avatars (mặc định: misc)
     * @return CloudFront URL của ảnh
     */
    @PostMapping
    public ResponseEntity<ApiResponse<String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "misc") String folder) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Vui lòng chọn file ảnh!"));
        }

        try {
            String fileUrl = s3Service.uploadImage(file, folder);
            log.info("Uploaded image to S3: folder={}, url={}", folder, fileUrl);
            return ResponseEntity.ok(ApiResponse.success("Upload thành công", fileUrl));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IOException | RuntimeException e) {
            log.error("Upload failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Lỗi khi upload ảnh: " + e.getMessage()));
        }
    }
}
