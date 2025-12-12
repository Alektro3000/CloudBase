package com.al3000.CloudBase.dto;

public record FilePath(String username, String path) {
    public String getPrefix() {
        return "user-" + username + "/";
    }

    public String getFilePath() {
        return getPrefix() + path;
    }
}
