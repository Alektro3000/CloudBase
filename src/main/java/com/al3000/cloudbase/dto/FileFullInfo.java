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
        return new FilePath(user, path + name + (isDir() ? "/" : ""));
    }

    public FileInfo getFileInfo() {
        //Don't ask
        return new FileInfo(path, name + (isDir() ? "/" : ""), size, isDirectory ? "Directory" : "File");
    }
    public boolean isRootOfFolder(FilePath filePath) {
        return (path + name).equals(filePath.path())  ||
                (path + name + "/").equals(filePath.path()) ;

    }
    public boolean isDir() {
        return isDirectory;
    }
    public boolean isFile() {
        return !isDir();
    }
}
