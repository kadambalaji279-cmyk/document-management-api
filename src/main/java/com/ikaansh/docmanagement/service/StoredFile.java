package com.ikaansh.docmanagement.service;

public class StoredFile {

    private final String path;
    private final String fileName;

    public StoredFile(String path, String fileName) {
        this.path = path;
        this.fileName = fileName;
    }

    public String getPath() {
        return path;
    }

    public String getFileName() {
        return fileName;
    }
}
