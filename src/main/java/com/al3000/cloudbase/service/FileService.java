package com.al3000.cloudbase.service;

import com.al3000.cloudbase.dto.FileFullInfo;
import com.al3000.cloudbase.dto.FileInfo;
import com.al3000.cloudbase.dto.FilePath;
import com.al3000.cloudbase.exception.DestinationAlreadyExist;
import com.al3000.cloudbase.exception.FileDoesNotExist;
import com.al3000.cloudbase.exception.InternalServerException;
import com.al3000.cloudbase.repository.FileRepository;
import com.al3000.cloudbase.service.search.StringSearchAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.hibernate.internal.util.collections.CollectionHelper.listOf;

@Service
@RequiredArgsConstructor
public class FileService {
    final private FileRepository fileRepository;
    final private StringSearchAlgorithm searchService;

    public void addRecursivelyFolders(FilePath path) throws InternalServerException {
        String[] parts = path.path().split("/");


        StringBuilder current = new StringBuilder();

        var count = parts.length;
        if (!path.isDir())
            count--;

        for (int i = 0; i < count; i++) {
            current.append(parts[i]);
            current.append("/");
            var subpath = new FilePath(path.username(), current.toString());
            fileRepository.createFolder(subpath);
        }
    }

    public void uploadFile(MultipartFile file, FilePath path) throws InternalServerException {
        addRecursivelyFolders(path);
        fileRepository.uploadFile(file, path);
    }

    public Stream<FileInfo> getFolderFiles(FilePath filePath) {
        return fileRepository.getFolderContent(filePath, false)
                .filter(x -> !x.isRootOfFolder(filePath))
                .map(FileFullInfo::getFileInfo);
    }

    public void removeFile(FilePath path) {
        if (path.path().endsWith("/")) {
            var removeList = fileRepository.getFolderContent(path, true)
                    .map(FileFullInfo::getFilePath).toList();
            fileRepository.removeFiles(removeList);
        } else {
            fileRepository.removeFiles(listOf(path));
        }
    }

    public FileInfo move(FilePath path, FilePath target) throws DestinationAlreadyExist, InternalServerException {
        List<Pair<FilePath, FilePath>> renameList;
        if (path.path().endsWith("/")) {
            renameList = fileRepository.getFolderContent(path, true)
                    .map(object -> {
                                var sourcePath = object.getFilePath();
                                var targetPath = new FilePath(sourcePath.username(),
                                        sourcePath.path().replace(path.path(), target.path()));
                                return Pair.of(sourcePath, targetPath);
                            }
                    ).toList();
        } else {
            renameList = listOf(Pair.of(path, target));
        }

        if (renameList.stream().anyMatch(x -> fileRepository.fileExist(x.getSecond())))
            throw new DestinationAlreadyExist("File already exists");

        try {
            addRecursivelyFolders(target);

            for (var entry : renameList) {
                fileRepository.copyObject(entry.getFirst(), entry.getSecond());
            }
            fileRepository.removeFiles(renameList.stream()
                    .map(Pair::getFirst)
                    .collect(Collectors.toList()));

            return fileRepository.getFileInformation(target).getFileInfo();
        } catch (Exception e) {
            throw new InternalServerException(e.getMessage());
        }

    }

    public Stream<FileInfo> findFiles(FilePath filePath, String query) {
        return fileRepository.getFolderContent(filePath, true)
                .filter(x -> searchService.contains(x.name(),query))
                .filter(FileFullInfo::isFile)
                .map(FileFullInfo::getFileInfo);
    }

    public InputStreamResource downloadObject(FilePath path) throws FileDoesNotExist {
        //We can't know is it file or not
        var info = fileRepository.getFileInformation(path);

        if (info.isFile()) {
            InputStream inputStream = fileRepository.downloadFile(path);
            return new InputStreamResource(inputStream);
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zipOut = new ZipOutputStream(baos)) {

            downloadFolder(path, zipOut);
            return new InputStreamResource(
                    new ByteArrayInputStream(baos.toByteArray()));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    void downloadFolder(FilePath path, ZipOutputStream zipOut) {

        fileRepository.getFolderContent(path, true).forEach((file) -> {
            try {
                // skip folder markers
                if (file.isDir()) return;

                // Read object content
                try (InputStream in = fileRepository.downloadFile(file.getFilePath())
                ) {
                    // ZIP entry name should be relative to the folder
                    String entryName = file.getFilePath().path().substring(path.path().length() + 1);

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
        return fileRepository.createFolder(filePath).getFileInfo();
    }
}
