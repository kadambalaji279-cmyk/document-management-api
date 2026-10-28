package com.ikaansh.docmanagement.service;

import com.ikaansh.docmanagement.dto.DocumentRequest;
import com.ikaansh.docmanagement.dto.DocumentResponse;
import com.ikaansh.docmanagement.dto.PageResponse;
import com.ikaansh.docmanagement.entity.Document;
import com.ikaansh.docmanagement.entity.User;
import com.ikaansh.docmanagement.exception.ResourceNotFoundException;
import com.ikaansh.docmanagement.repository.DocumentRepository;
import com.ikaansh.docmanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private DocumentService documentService;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(10L);
        owner.setUsername("alice");
        owner.setEmail("alice@example.com");
        owner.setRole("USER");
    }

    @Test
    void create_shouldSaveDocument() {
        DocumentRequest request = documentRequest("My report", "Quarterly report", 10L);

        when(userRepository.findById(10L)).thenReturn(Optional.of(owner));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document documentToSave = invocation.getArgument(0);
            documentToSave.setId(100L);
            return documentToSave;
        });

        DocumentResponse response = documentService.create(request);

        assertEquals(100L, response.getId());
        assertEquals("My report", response.getTitle());
        assertEquals(10L, response.getOwnerId());
        assertEquals("alice", response.getOwnerUsername());
        verify(documentRepository).save(any(Document.class));
    }

    @Test
    void create_shouldThrowWhenOwnerDoesNotExist() {
        DocumentRequest request = documentRequest("My report", "Quarterly report", 99L);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> documentService.create(request)
        );

        assertEquals("User not found with id: 99", exception.getMessage());
        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void getById_shouldReturnDocument() {
        Document document = document(100L, "My report", owner);
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));

        DocumentResponse response = documentService.getById(100L);

        assertEquals(100L, response.getId());
        assertEquals("My report", response.getTitle());
        assertEquals("alice", response.getOwnerUsername());
    }

    @Test
    void getById_shouldThrowWhenDocumentDoesNotExist() {
        when(documentRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> documentService.getById(999L)
        );

        assertEquals("Document not found with id: 999", exception.getMessage());
    }

    @Test
    void list_shouldReturnPageResponseAndFilterByTitle() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Document document = document(100L, "Annual report", owner);
        Page<Document> page = new PageImpl<>(List.of(document), pageable, 1);

        when(documentRepository.findByTitleContainingIgnoreCase("report", pageable)).thenReturn(page);

        PageResponse<DocumentResponse> response = documentService.list("  report  ", pageable);

        assertEquals(1, response.getContent().size());
        assertEquals("Annual report", response.getContent().get(0).getTitle());
        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        verify(documentRepository).findByTitleContainingIgnoreCase("report", pageable);
    }

    @Test
    void list_shouldReturnEverythingWhenTitleIsBlank() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<Document> page = new PageImpl<>(List.of(document(100L, "One", owner)), pageable, 1);

        when(documentRepository.findAll(pageable)).thenReturn(page);

        PageResponse<DocumentResponse> response = documentService.list(" ", pageable);

        assertEquals(1, response.getContent().size());
        verify(documentRepository).findAll(pageable);
        verify(documentRepository, never()).findByTitleContainingIgnoreCase(any(), any());
    }

    @Test
    void update_shouldChangeDocumentFields() {
        Document document = document(100L, "Old title", owner);
        User newOwner = new User();
        newOwner.setId(20L);
        newOwner.setUsername("bob");

        DocumentRequest request = documentRequest("New title", "New description", 20L);
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));
        when(userRepository.findById(20L)).thenReturn(Optional.of(newOwner));
        when(documentRepository.saveAndFlush(document)).thenReturn(document);

        DocumentResponse response = documentService.update(100L, request);

        assertEquals("New title", response.getTitle());
        assertEquals("New description", response.getDescription());
        assertEquals(20L, response.getOwnerId());
        assertSame(newOwner, document.getOwner());
        verify(documentRepository).saveAndFlush(document);
    }

    @Test
    void update_shouldThrowWhenDocumentDoesNotExist() {
        DocumentRequest request = documentRequest("New title", "New description", 10L);
        when(documentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> documentService.update(999L, request));
        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void delete_shouldRemoveDocumentAndFile() {
        Document document = document(100L, "My report", owner);
        document.setFilePath("C:/uploads/file.pdf");
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));

        documentService.delete(100L);

        verify(documentRepository).delete(document);
        verify(fileStorageService).deleteFile("C:/uploads/file.pdf");
    }

    @Test
    void delete_shouldThrowWhenDocumentDoesNotExist() {
        when(documentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> documentService.delete(999L));
        verify(documentRepository, never()).delete(any(Document.class));
    }

    @Test
    void upload_shouldStoreFileMetadataOnDocument() {
        Document document = document(100L, "My report", owner);
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "pdf-content".getBytes(StandardCharsets.UTF_8));

        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));
        when(fileStorageService.store(file)).thenReturn(new StoredFile("C:/uploads/uuid.pdf", "report.pdf"));
        when(documentRepository.saveAndFlush(document)).thenReturn(document);

        DocumentResponse response = documentService.upload(100L, file);

        assertEquals("report.pdf", response.getFileName());
        assertEquals("application/pdf", response.getContentType());
        assertEquals(11L, response.getFileSize());
        verify(fileStorageService).store(file);
        verify(documentRepository).saveAndFlush(document);
    }

    private DocumentRequest documentRequest(String title, String description, Long ownerId) {
        DocumentRequest request = new DocumentRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setOwnerId(ownerId);
        return request;
    }

    private Document document(Long id, String title, User documentOwner) {
        Document document = new Document();
        document.setId(id);
        document.setTitle(title);
        document.setDescription("Description");
        document.setOwner(documentOwner);
        return document;
    }
}
