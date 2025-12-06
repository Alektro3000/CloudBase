package com.al3000.CloudBase.controller;

import com.al3000.CloudBase.dto.FileInfo;
import com.al3000.CloudBase.dto.FilePath;
import com.al3000.CloudBase.service.FileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipOutputStream;

@RestController
public class File {
    FileRepository fileRepository;
    @Autowired
    File(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    @PostMapping(value = "/api/resource",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file, Authentication authentication, @RequestParam String path)
    {
        fileRepository.uploadFile(file, new FilePath(authentication.getName(), path));
        return ResponseEntity.status(HttpStatus.CREATED).body("result");
    }

    @DeleteMapping("/api/resource")
    public ResponseEntity<String> delete(Authentication authentication, @RequestParam String path)
    {
        fileRepository.removeFile( new FilePath(authentication.getName(), path));
        return ResponseEntity.status(HttpStatus.CREATED).body("result");
    }

    @GetMapping("/api/resource/search")
    public ResponseEntity<List<FileInfo>> search(Authentication authentication, @RequestParam String query)
    {
        var stream = fileRepository.findFiles(new FilePath(authentication.getName(),""), query);
        return ResponseEntity.ok(stream.toList());
    }

    @GetMapping("/api/resource/move")
    public ResponseEntity<FileInfo> move(Authentication authentication, @RequestParam String from, @RequestParam String to)
    {
        var result = fileRepository.moveFile(
                new FilePath(authentication.getName(),from),
                new FilePath(authentication.getName(),to)
            );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/resource/download")
    public ResponseEntity<Resource> download(Authentication authentication, @RequestParam String path) throws IOException {
        var filePath = new FilePath(authentication.getName(), path);
        var info = fileRepository.getFileInformation(filePath);
        if("FILE".equalsIgnoreCase(info.getType())) {
            InputStream inputStream = fileRepository.downloadFile( new FilePath(authentication.getName(),path));

            InputStreamResource resource = new InputStreamResource(inputStream);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + path + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        }
        else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zipOut = new ZipOutputStream(baos);
            fileRepository.downloadFolder( new FilePath(authentication.getName(),path), zipOut);
            zipOut.close();

            var inputStreamResource = new InputStreamResource(
                    new ByteArrayInputStream(baos.toByteArray())
            );


            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + path + ".zip\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(inputStreamResource);
        }
    }
}
