package com.al3000.cloudbase.controller;

import com.al3000.cloudbase.dto.FileInfo;
import com.al3000.cloudbase.dto.FilePath;
import com.al3000.cloudbase.exception.InternalServerException;
import com.al3000.cloudbase.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/directory")
public class DirectoryController {
    private final FileService fileService;

    @GetMapping
    public ResponseEntity<List<FileInfo>> getFolderContent(Authentication authentication, @RequestParam String path) {
        var folderContent = fileService.getFolderFiles(new FilePath(authentication.getName(), path));
        return ResponseEntity.ok().body(folderContent.toList());
    }

    @PostMapping
    public ResponseEntity<FileInfo> createFolder(Authentication authentication, @RequestParam String path) throws InternalServerException {
        FileInfo result = fileService.createFolder(new FilePath(authentication.getName(), path));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
