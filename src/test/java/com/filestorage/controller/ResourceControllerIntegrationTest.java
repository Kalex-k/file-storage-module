package com.filestorage.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.filestorage.model.Project;
import com.filestorage.model.Resource;
import com.filestorage.model.ResourceStatus;
import com.filestorage.model.ResourceType;
import com.filestorage.model.User;
import com.filestorage.model.UserRole;
import com.filestorage.repository.ProjectRepository;
import com.filestorage.repository.ResourceRepository;
import com.filestorage.repository.UserRepository;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Resource Controller Integration Tests")
class ResourceControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private MinioClient minioClient;
    
    @Autowired
    private ResourceRepository resourceRepository;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    private Project testProject;
    private User testUser;
    private Resource testResource;
    private MockMultipartFile testFile;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
       
        testProject = Project.builder()
                .name("Test Project")
                .storageSize(BigInteger.ZERO)
                .maxStorageSize(BigInteger.valueOf(2_147_483_648L)) // 2GB
                .build();
        testProject = projectRepository.save(testProject);
       
        testUser = User.builder()
                .username("testuser")
                .nickname("Test User")
                .roles(new ArrayList<>(List.of(UserRole.DEVELOPER, UserRole.MANAGER)))
                .build();
        testUser = userRepository.save(testUser);
        
        testResource = Resource.builder()
                .name("test-file.pdf")
                .key("project-1/test-key.pdf")
                .size(BigInteger.valueOf(1024L))
                .contentType("application/pdf")
                .type(ResourceType.PDF)
                .status(ResourceStatus.ACTIVE)
                .project(testProject)
                .createdBy(testUser)
                .updatedBy(testUser)
                .allowedRoles(new ArrayList<>(List.of(UserRole.DEVELOPER)))
                .build();
        testResource = resourceRepository.save(testResource);
        
        testFile = new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                "Test PDF content for integration test".getBytes()
        );
        
        // Mock MinIO operations
        try {
            doAnswer(invocation -> null).when(minioClient).putObject(any(PutObjectArgs.class));
        } catch (Exception e) {
            // Ignore
        }
    }
    
    @Test
    @DisplayName("Should upload file successfully")
    void shouldUploadFileSuccessfully() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(
                multipart("/api/v1/projects/{projectId}/resources", testProject.getId())
                        .file(testFile)
                        .param("allowedRoles", "DEVELOPER")
                        .header("x-user-id", testUser.getId())
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("test-document.pdf"))
                .andReturn();
        
        // Verify database state
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);
        Long resourceId = responseJson.get("id").asLong();
        
        Optional<Resource> savedResource = resourceRepository.findById(resourceId);
        assertTrue(savedResource.isPresent());
        assertEquals("test-document.pdf", savedResource.get().getName());
        assertEquals(ResourceStatus.ACTIVE, savedResource.get().getStatus());
        
        // Verify MinIO interaction
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }
    
    @Test
    @DisplayName("Should return 413 when file too large")
    void shouldReturn413WhenFileTooLarge() throws Exception {
        // Given
        long oversizedBytes = 501L * 1024 * 1024; // 501MB
        MockMultipartFile largeFile = new OversizedMockMultipartFile(
                "file",
                "large.zip",
                "application/zip",
                "placeholder".getBytes(),
                oversizedBytes
        );
        
        // When & Then
        mockMvc.perform(
                multipart("/api/v1/projects/{projectId}/resources", testProject.getId())
                        .file(largeFile)
                        .header("x-user-id", testUser.getId())
        )
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.errorCode").value("413 PAYLOAD_TOO_LARGE"));
    }
    
    @Test
    @DisplayName("Should download file successfully")
    void shouldDownloadFileSuccessfully() throws Exception {
        // Given
        InputStream mockInputStream = new ByteArrayInputStream("file content".getBytes());
        GetObjectResponse mockResponse = mock(GetObjectResponse.class);
        
        doAnswer(invocation -> mockResponse).when(minioClient).getObject(any(GetObjectArgs.class));
        
        // When & Then
        mockMvc.perform(
                get("/api/v1/projects/{projectId}/resources/{resourceId}/download",
                        testProject.getId(), testResource.getId())
                        .header("x-user-id", testUser.getId())
        )
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("test-file.pdf")));
        
        verify(minioClient).getObject(any(GetObjectArgs.class));
    }
    
    @Test
    @DisplayName("Should delete file successfully")
    void shouldDeleteFileSuccessfully() throws Exception {
        // Given
        try {
            doAnswer(invocation -> null).when(minioClient).removeObject(any(RemoveObjectArgs.class));
        } catch (Exception e) {
            // Ignore
        }
        
        // When
        mockMvc.perform(
                delete("/api/v1/projects/{projectId}/resources/{resourceId}",
                        testProject.getId(), testResource.getId())
                        .header("x-user-id", testUser.getId())
        )
                .andExpect(status().isNoContent());
        
        // Then
        Optional<Resource> deletedResource = resourceRepository.findById(testResource.getId());
        assertTrue(deletedResource.isPresent());
        assertEquals(ResourceStatus.DELETED, deletedResource.get().getStatus());
        assertNull(deletedResource.get().getKey());
        assertEquals(BigInteger.ZERO, deletedResource.get().getSize());
        
        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }
    
    @Test
    @DisplayName("Should return 403 when user lacks permission")
    void shouldReturn403WhenUserLacksPermission() throws Exception {
        // Given - Create unauthorized user
        User unauthorizedUser = User.builder()
                .username("unauthorized")
                .nickname("Unauthorized")
                .roles(new ArrayList<>(List.of(UserRole.TESTER)))
                .build();
        unauthorizedUser = userRepository.save(unauthorizedUser);
        
        // When & Then
        mockMvc.perform(
                delete("/api/v1/projects/{projectId}/resources/{resourceId}",
                        testProject.getId(), testResource.getId())
                        .header("x-user-id", unauthorizedUser.getId())
        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value(
                        containsString("Only file creator or project manager can delete files")));
    }
    
    @Test
    @DisplayName("Should get project files with pagination")
    void shouldGetProjectFilesWithPagination() throws Exception {
        // When & Then
        mockMvc.perform(
                get("/api/v1/projects/{projectId}/resources", testProject.getId())
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc")
                        .header("x-user-id", testUser.getId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.pageable.pageSize").value(10));
    }
    
    @Test
    @DisplayName("Should upload multiple files successfully")
    void shouldUploadMultipleFilesSuccessfully() throws Exception {
        // Given
        MockMultipartFile file1 = new MockMultipartFile(
                "files", "doc1.pdf", "application/pdf", "content1".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "files", "doc2.docx", 
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 
                "content2".getBytes()
        );
        
        // When & Then
        mockMvc.perform(
                multipart("/api/v1/projects/{projectId}/resources/bulk", testProject.getId())
                        .file(file1)
                        .file(file2)
                        .header("x-user-id", testUser.getId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("doc1.pdf"))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$[1].name").value("doc2.docx"))
                .andExpect(jsonPath("$[1].status").value("SUCCESS"));
        
        verify(minioClient, atLeast(1)).putObject(any(PutObjectArgs.class));
    }
    
    @Test
    @DisplayName("Should generate presigned URL")
    void shouldGeneratePresignedUrl() throws Exception {
        // Given
        String expectedUrl = "https://minio.example.com/bucket/file?X-Amz-Signature=abc123";
        doAnswer(invocation -> expectedUrl)
                .when(minioClient).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
        
        // When & Then
        mockMvc.perform(
                get("/api/v1/projects/{projectId}/resources/{resourceId}/url",
                        testProject.getId(), testResource.getId())
                        .header("x-user-id", testUser.getId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(expectedUrl))
                .andExpect(jsonPath("$.expiresIn").value(3600));
        
        verify(minioClient).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
    }
    
    @Test
    @DisplayName("Should return 404 when resource not found")
    void shouldReturn404WhenResourceNotFound() throws Exception {
        // When & Then
        mockMvc.perform(
                get("/api/v1/projects/{projectId}/resources/{resourceId}/download",
                        testProject.getId(), 99999L)
                        .header("x-user-id", testUser.getId())
        )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    private static class OversizedMockMultipartFile extends MockMultipartFile {

        private final long reportedSize;

        OversizedMockMultipartFile(String name, String originalFilename,
                                   String contentType, byte[] content, long reportedSize) {
            super(name, originalFilename, contentType, content);
            this.reportedSize = reportedSize;
        }

        @Override
        public long getSize() {
            return reportedSize;
        }
    }
}
