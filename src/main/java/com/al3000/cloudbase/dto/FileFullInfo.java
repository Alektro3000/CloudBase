package com.al3000.cloudbase.dto;


import jakarta.validation.constraints.NotEmpty;

public record FileFullInfo(String user, String path, @NotEmpty String name, Long size, boolean isDirectory) {

    public FileFullInfo {
        if (!path.endsWith("/") && !path.isEmpty()) {
            throw new IllegalArgumentException("Non empty path must end with '/', current path is " + path);
        }
        if (user.contains("/")) {
            throw new IllegalArgumentException("User name can't contain '/', current name is " + user);
        }
        if (name.contains("/")) {
            throw new IllegalArgumentException("Name can't contain '/', current name is " + name);
        }
    }

    public FilePath getFilePath() {
        return new FilePath(user,
                path + name + (isDir() ? "/" : "") //Dir name must end with / to comply with specification
        );
    }

    public FileInfo getFileInfo() {
        return new FileInfo(path,
                name + (isDir() ? "/" : ""), //Dir name must end with / to comply with specification
                size,
                isDirectory ? "DIRECTORY" : "FILE");
    }
    public boolean isRootOfFolder(FilePath filePath) {
        return (path + name).equals(filePath.path())  || //To cover case of root folder
                (path + name + "/").equals(filePath.path()) ;

    }
    public boolean isDir() {
        return isDirectory;
    }
    public boolean isFile() {
        return !isDir();
    }
}
