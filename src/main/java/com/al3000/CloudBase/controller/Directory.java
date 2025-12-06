package com.al3000.CloudBase.controller;

import com.al3000.CloudBase.dto.FileInfo;
import com.al3000.CloudBase.dto.FilePath;
import com.al3000.CloudBase.service.FileRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@RestController()
public class Directory {
    FileRepository fileRepository;
    @Autowired
    Directory(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    @GetMapping( "api/directory")
    public ResponseEntity<List<FileInfo>> getFolderContent(Authentication authentication, @RequestParam String path) {
        var folderContent = fileRepository.getFolderContent(new FilePath(authentication.getName(), path));
        return ResponseEntity.ok().body(folderContent.toList());
    }

    @PostMapping("api/directory")
    public ResponseEntity<FileInfo> createFolder(Authentication authentication, @RequestParam String path) {
        var result = fileRepository.createFolder(new FilePath(authentication.getName(), path));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
