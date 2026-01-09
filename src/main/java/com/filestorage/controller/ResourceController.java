package com.filestorage.controller;

import com.filestorage.dto.FileDownloadResponse;
import com.filestorage.dto.ResourceDto;
import com.filestorage.dto.ResourceResponse;
import com.filestorage.dto.ResourceUploadStatus;
import com.filestorage.model.Resource;
import com.filestorage.model.UserRole;
import com.filestorage.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/resources")
@Slf4j
@Validated
@RequiredArgsConstructor
public class ResourceController {

    private final FileStorageService fileStorageService;

    @Value("${file-storage.presigned-url-expiry-seconds}")
    private int presignedUrlExpirySeconds;

    @Value("${file-storage.bulk-upload-max-files}")
    private int bulkUploadMaxFiles;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResourceResponse> uploadFile(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Set<UserRole> allowedRoles,
            @RequestHeader("x-user-id") Long userId) {

        log.info("Upload request: project={}, file={}, size={}",
                projectId, file.getOriginalFilename(), file.getSize());

        Resource resource = fileStorageService.uploadFile(file, projectId, userId, allowedRoles);

        return ResponseEntity.status(HttpStatus.CREATED).body(ResourceResponse.from(resource));
    }

    @GetMapping("/{resourceId}/download")
    public ResponseEntity<StreamingResponseBody> downloadFile(
            @PathVariable Long projectId,
            @PathVariable Long resourceId,
            @RequestHeader("x-user-id") Long userId) throws AccessDeniedException {
        
        FileDownloadResponse download = fileStorageService.downloadFile(
                resourceId, projectId, userId);
        
        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.getFileName() + "\"");
        
        if (download.getSize() != null && download.getSize() > 0) {
            responseBuilder.contentLength(download.getSize());
        }
        
        StreamingResponseBody stream = outputStream -> {
            try (var inputStream = download.getInputStream()) {
                inputStream.transferTo(outputStream);
            }
        };
        
        return responseBuilder.body(stream);
    }

    @GetMapping("/{resourceId}/url")
    public ResponseEntity<Map<String, Object>> getDownloadUrl(
            @PathVariable Long projectId,
            @PathVariable Long resourceId,
            @RequestHeader("x-user-id") Long userId) throws AccessDeniedException {
        String url = fileStorageService.generatePresignedUrl(resourceId, projectId, userId);

        return ResponseEntity.ok(Map.of("url", url, "expiresIn", presignedUrlExpirySeconds));
    }

    @DeleteMapping("/{resourceId}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable Long projectId,
            @PathVariable Long resourceId,
            @RequestHeader("x-user-id") Long userId) throws AccessDeniedException {

        log.info("Delete request: project={}, resource={}, user={}", projectId, resourceId, userId);

        fileStorageService.deleteFile(resourceId, projectId, userId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<ResourceDto>> getProjectFiles(
            @PathVariable Long projectId,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = org.springframework.data.domain.Sort.Direction.DESC
            ) Pageable pageable,
            @RequestHeader("x-user-id") Long userId) {

        Page<ResourceDto> resources = fileStorageService.getProjectFiles(
                projectId, userId, pageable);

        return ResponseEntity.ok(resources);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<ResourceResponse>> uploadMultipleFiles(
            @PathVariable Long projectId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(required = false) Set<UserRole> allowedRoles,
            @RequestHeader("x-user-id") Long userId) {

        log.info("Bulk upload: project={}, files={}", projectId, files.size());

        if (files.size() > bulkUploadMaxFiles) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    String.format("Maximum %d files can be uploaded at once", bulkUploadMaxFiles));
        }

        List<ResourceResponse> responses = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                Resource resource = fileStorageService.uploadFile(
                        file, projectId, userId, allowedRoles);

                responses.add(ResourceResponse.from(resource, ResourceUploadStatus.SUCCESS));

            } catch (Exception e) {
                log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
                responses.add(ResourceResponse.builder()
                        .name(file.getOriginalFilename())
                        .status(ResourceUploadStatus.FAILED)
                        .error(e.getMessage())
                        .build());
            }
        }
        return ResponseEntity.ok(responses);
    }

}
