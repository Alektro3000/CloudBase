package com.al3000.cloudbase.dto;

import java.io.FileNotFoundException;

public record FilePath(String username, String path) {
    public String getPrefix() {
        return "user-" + username + "/";
    }

    public String getFullPath() {
        return getPrefix() + path;
    }

    public FilePath(String fullName) {
        this(extractUsername(fullName), extractPath(fullName));
    }

    private static String extractUsername(String fullName) {
        validateFullName(fullName);
        return fullName.substring(5, fullName.indexOf('/'));
    }

    private static String extractPath(String fullName) {
        validateFullName(fullName);
        return fullName.substring(fullName.indexOf('/') + 1);
    }

    private static void validateFullName(String fullName) {
        if (!fullName.startsWith("user-") || !fullName.contains("/")) {
            throw new IllegalArgumentException("fullName '" + fullName + "' is malformed");
        }
    }

    public void throwIsNotFile() {
        throw new  IllegalArgumentException("Path '" + getFullPath() + "' is not a file");
    }

    public void throwIsNotDirectory() {
        throw new  IllegalArgumentException("Path '" + getFullPath() + "' is not a directory");
    }

    public String getDirectoryPath() {
        if(!isDir())
            throwIsNotDirectory();
        var substrIndex = path().lastIndexOf("/", path().length() - 2);
        return path().substring(0, substrIndex+1);
    }
    public String getDirectoryName() {
        if(!isDir())
            throwIsNotDirectory();
        var substrIndex = path().lastIndexOf("/", path().length() - 2);
        return path().substring(substrIndex+1, path().length()-1);
    }

    public String getFilePath() {
        if(isDir())
            throwIsNotFile();
        var substrIndex = path().lastIndexOf("/");
        return path().substring(0, substrIndex+1);
    }
    public String getFileName() {
        if(isDir())
            throwIsNotFile();
        var substrIndex = path().lastIndexOf("/");
        return path().substring(substrIndex+1);
    }

    public boolean isDir() {
        return path.endsWith("/");
    }
}
