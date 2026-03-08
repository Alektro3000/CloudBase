package com.al3000.cloudbase.dto;

public record FilePath(String username, String path) {
    public String getPrefix() {
        return "user-" + username + "/";
    }

    public String getFullPath() {
        return getPrefix() + path;
    }

    public FilePath(String fullName) {
        this(fullName.substring(5,fullName.indexOf('/')), fullName.substring(fullName.indexOf('/')+1));
    }

    public String getDirectoryPath() {
        var substrIndex = path().lastIndexOf("/", path().length() - 2);
        return path().substring(0, substrIndex+1);
    }
    public String getDirectoryName() {
        var substrIndex = path().lastIndexOf("/", path().length() - 2);
        return path().substring(substrIndex+1, path().length()-1);
    }

    public String getFilePath() {
        var substrIndex = path().lastIndexOf("/");
        return path().substring(0, substrIndex+1);
    }
    public String getFileName() {
        var substrIndex = path().lastIndexOf("/");
        return path().substring(substrIndex+1);
    }

    public Boolean isDir() {
        return path.endsWith("/");
    }
}
