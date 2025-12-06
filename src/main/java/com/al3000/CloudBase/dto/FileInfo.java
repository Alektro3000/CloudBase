package com.al3000.CloudBase.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class FileInfo {
    String path;
    String name;
    Long size;
    String type;

    public String getFullPath()
    {
        return path + name;
    }
}
