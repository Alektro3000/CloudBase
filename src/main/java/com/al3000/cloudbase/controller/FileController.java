package com.al3000.cloudbase.controller;

import com.al3000.cloudbase.dto.FileInfo;
import com.al3000.cloudbase.dto.FilePath;
import com.al3000.cloudbase.exception.DestinationAlreadyExist;
import com.al3000.cloudbase.exception.FileDoesNotExist;
import com.al3000.cloudbase.exception.InternalServerException;
import com.al3000.cloudbase.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/resource")
public class FileController {
    public final FileService fileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> upload(@RequestParam("object") MultipartFile file, Authentication authentication, @RequestParam String path) throws InternalServerException {
        fileService.uploadFile(file, new FilePath(authentication.getName(), path));
        return ResponseEntity.status(HttpStatus.CREATED).body("result");
    }

    @DeleteMapping()
    public ResponseEntity<String> delete(Authentication authentication, @RequestParam String path) {
        fileService.removeFile(new FilePath(authentication.getName(), path));
        return ResponseEntity.status(HttpStatus.CREATED).body("result");
    }

    @GetMapping("/search")
    public ResponseEntity<List<FileInfo>> search(Authentication authentication, @RequestParam String query) {
        var stream = fileService.findFiles(new FilePath(authentication.getName(), ""), query);
        return ResponseEntity.ok(stream.toList());
    }

    @GetMapping("/move")
    public ResponseEntity<FileInfo> move(Authentication authentication, @RequestParam String from, @RequestParam String to) throws InternalServerException, DestinationAlreadyExist {
        var result = fileService.moveFile(
                new FilePath(authentication.getName(), from),
                new FilePath(authentication.getName(), to)
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> download(Authentication authentication, @RequestParam String path) throws FileDoesNotExist {
        var inputStreamResource = fileService.downloadObject(new FilePath(authentication.getName(), path));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + path + ".zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(inputStreamResource);

    }
}
