package com.ikaansh.docmanagement.service;

import com.ikaansh.docmanagement.dto.DocumentRequest;
import com.ikaansh.docmanagement.dto.DocumentResponse;
import com.ikaansh.docmanagement.dto.FileDownload;
import com.ikaansh.docmanagement.dto.PageResponse;
import com.ikaansh.docmanagement.entity.Document;
import com.ikaansh.docmanagement.entity.User;
import com.ikaansh.docmanagement.exception.ResourceNotFoundException;
import com.ikaansh.docmanagement.repository.DocumentRepository;
import com.ikaansh.docmanagement.repository.UserRepository;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public DocumentService(DocumentRepository documentRepository,
                           UserRepository userRepository,
                           FileStorageService fileStorageService) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public PageResponse<DocumentResponse> list(String title, Pageable pageable) {
        Page<Document> documents;
        if (title == null || title.isBlank()) {
            documents = documentRepository.findAll(pageable);
        } else {
            documents = documentRepository.findByTitleContainingIgnoreCase(title.trim(), pageable);
        }
        return PageResponse.from(documents.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public DocumentResponse getById(Long id) {
        return toResponse(findDocument(id));
    }

    @Transactional
    public DocumentResponse create(DocumentRequest request) {
        User owner = findUser(request.getOwnerId());

        Document document = new Document();
        document.setTitle(request.getTitle());
        document.setDescription(request.getDescription());
        document.setOwner(owner);

        return toResponse(documentRepository.save(document));
    }

    @Transactional
    public DocumentResponse update(Long id, DocumentRequest request) {
        Document document = findDocument(id);
        User owner = findUser(request.getOwnerId());

        document.setTitle(request.getTitle());
        document.setDescription(request.getDescription());
        document.setOwner(owner);

        // Flush so @PreUpdate runs before we build the response with the new updatedAt value.
        return toResponse(documentRepository.saveAndFlush(document));
    }

    @Transactional
    public void delete(Long id) {
        Document document = findDocument(id);
        String filePath = document.getFilePath();

        documentRepository.delete(document);
        fileStorageService.deleteFile(filePath);
    }

    @Transactional
    public DocumentResponse upload(Long id, MultipartFile file) {
        Document document = findDocument(id);
        StoredFile storedFile = fileStorageService.store(file);

        String previousFilePath = document.getFilePath();
        document.setFileName(storedFile.getFileName());
        document.setFilePath(storedFile.getPath());
        document.setContentType(limitContentType(file.getContentType()));
        document.setFileSize(file.getSize());

        // Flush so @PreUpdate runs before we build the response with the new updatedAt value.
        Document saved = documentRepository.saveAndFlush(document);

        // Replacing an upload should not leave the old file behind.
        if (previousFilePath != null && !previousFilePath.equals(storedFile.getPath())) {
            fileStorageService.deleteFile(previousFilePath);
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public FileDownload download(Long id) {
        Document document = findDocument(id);
        Resource resource = fileStorageService.loadAsResource(document.getFilePath());

        return new FileDownload(
                resource,
                document.getFileName(),
                document.getContentType(),
                document.getFileSize()
        );
    }

    private Document findDocument(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    // content_type is VARCHAR(100); ignore anything longer instead of failing the upload.
    private String limitContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        return contentType.length() > 100 ? contentType.substring(0, 100) : contentType;
    }

    private DocumentResponse toResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getDescription(),
                document.getFileName(),
                document.getContentType(),
                document.getFileSize(),
                document.getOwner().getId(),
                document.getOwner().getUsername(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
