package com.al3000.cloudbase.dto;


import jakarta.validation.constraints.NotEmpty;

public record FileFullInfo(String user, String path, @NotEmpty String name, Long size, String type) {

    public FileFullInfo {
        if (path.endsWith("/")) {
            throw new IllegalArgumentException("Path must not end with '/'");
        }
    }
    public FilePath getFilePath() {
        return new FilePath(user, path + "/" + name);
    }

    public FileInfo getFileInfo() {
        return new FileInfo(path, name, size, type);
    }
    public boolean isDir() {
        return "DIRECTORY".equals(type);
    }
    public boolean isFile() {
        return !isDir();
    }
}
