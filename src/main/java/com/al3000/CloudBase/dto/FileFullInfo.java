package com.al3000.CloudBase.dto;


public record FileFullInfo(String user, String path, String name, Long size, String type) {
    public FilePath getFilePath() {
        return new FilePath(user, path + name);
    }

    public FileInfo getFileInfo() {
        return new FileInfo(path, name, size, type);
    }
    public boolean isDir() {
        return "DIRECTORY".equals(type);
    }
}
