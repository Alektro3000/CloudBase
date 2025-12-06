package com.al3000.CloudBase.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class FilePath {
    private String username;
    @Getter
    private String path;
    public String getPrefix() {
        return "user-" + username + "/";
    }
    public String getFilePath() {
        return getPrefix() + path;
    }
    public String getDeletePath() {
        return getPrefix() + path.replaceAll("/+$", "");
    }
}
