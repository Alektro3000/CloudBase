package com.al3000.CloudBase.service;

import com.al3000.CloudBase.dto.FileFullInfo;
import com.al3000.CloudBase.dto.FileInfo;
import com.al3000.CloudBase.dto.FilePath;
import com.al3000.CloudBase.exception.DestinationAlreadyExist;
import com.al3000.CloudBase.exception.FileDoesNotExist;
import com.al3000.CloudBase.exception.InternalServerException;
import com.al3000.CloudBase.repository.FileRepository;
import io.minio.messages.DeleteObject;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class FileService {
    final private FileRepository fileRepository;

    public void addRecursivelyFolders(FilePath path) throws InternalServerException {
        String[] parts = path.path().split("/");

        StringBuilder current = new StringBuilder();

        for (int i = 0; i < parts.length - 1; i++) {
            if (!current.isEmpty()) {
                current.append("/");
            }
            current.append(parts[i]);
            createFolder(new FilePath(path.username(), current.toString()));
        }
    }

    public void uploadFile(MultipartFile file, FilePath path) throws InternalServerException {
        addRecursivelyFolders(path);
        fileRepository.uploadFile(file, path);
    }

    public Stream<FileInfo> getFolderFiles(FilePath filePath) {
        return fileRepository.getFolderContent(filePath, false)
                .filter(x -> !"".equalsIgnoreCase(x.name()))
                .map(FileFullInfo::getFileInfo);
    }

    public void removeFile(FilePath path) {
        var removeList = fileRepository.getFolderContent(path, true)
                .map(x ->
                        new DeleteObject(
                                path.getPrefix() + x.path() + x.name()
                        ))
                .toList();

        fileRepository.removeFiles(removeList);
    }

    public FileInfo moveFile(FilePath path, FilePath target) throws DestinationAlreadyExist, InternalServerException {


        List<Pair<FilePath,FilePath>> renameList = fileRepository.getFolderContent(path, true)
                .map(object -> {
                            var sourcePath = object.getFilePath();
                            var targetPath = new FilePath(sourcePath.username(),
                                    sourcePath.path().replace(path.path(), target.path()));
                            return Pair.of(sourcePath, targetPath);
                        }
                ).toList();

        if (renameList.stream().anyMatch(x -> fileRepository.fileExist(x.getSecond())))
            throw new DestinationAlreadyExist("File already exists");

        try {
            addRecursivelyFolders(target);

            for (var entry : renameList) {
                fileRepository.copyObject(entry.getFirst(), entry.getSecond());
            }
            fileRepository.removeFiles(renameList.stream()
                    .map(x -> new DeleteObject(x.getFirst().getFilePath()))
                    .collect(Collectors.toList()));

            return fileRepository.getFileInformation(target).getFileInfo();
        } catch (Exception e) {
            throw new InternalServerException(e.getMessage());
        }

    }

    public Stream<FileInfo> findFiles(FilePath filePath, String query) {
        return fileRepository.getFolderContent(filePath, true)
                .filter(x -> x.name().contains(query))
                .filter(x -> !"".equalsIgnoreCase(x.name()))
                .map(FileFullInfo::getFileInfo);
    }

    public InputStreamResource downloadObject(FilePath path) throws FileDoesNotExist {
        var info = fileRepository.getFileInformation(path);
        if (!info.isDir()) {

            InputStream inputStream = fileRepository.downloadFile(path);
            return new InputStreamResource(inputStream);

        } else {
            try(ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ZipOutputStream zipOut = new ZipOutputStream(baos)) {

                downloadFolder(path, zipOut);
                return new InputStreamResource(
                        new ByteArrayInputStream(baos.toByteArray()));

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void downloadFolder(FilePath path, ZipOutputStream zipOut) {

        fileRepository.getFolderContent(path, true).forEach((file) -> {
            try {
                // skip folder markers
                if (file.isDir()) return;

                // Read object content
                try (InputStream in = fileRepository.downloadFile(file.getFilePath())
                ) {
                    // ZIP entry name should be relative to the folder
                    String entryName = file.getFilePath().path().substring(path.path().length());

                    zipOut.putNextEntry(new ZipEntry(entryName));

                    in.transferTo(zipOut);

                    zipOut.closeEntry();
                }
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        });

    }

    public FileInfo createFolder(FilePath filePath) throws InternalServerException {
        addRecursivelyFolders(filePath);
        return fileRepository.createFolder(filePath).getFileInfo();
    }
}
