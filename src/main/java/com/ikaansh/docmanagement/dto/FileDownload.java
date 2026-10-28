package com.ikaansh.docmanagement.dto;

import org.springframework.core.io.Resource;

public class FileDownload {

    private final Resource resource;
    private final String fileName;
    private final String contentType;
    private final Long fileSize;

    public FileDownload(Resource resource, String fileName, String contentType, Long fileSize) {
        this.resource = resource;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
    }

    public Resource getResource() {
        return resource;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }
}
