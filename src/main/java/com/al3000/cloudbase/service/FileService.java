package com.al3000.cloudbase.service;

import com.al3000.cloudbase.dto.FileFullInfo;
import com.al3000.cloudbase.dto.FileInfo;
import com.al3000.cloudbase.dto.FilePath;
import com.al3000.cloudbase.exception.DestinationAlreadyExistsException;
import com.al3000.cloudbase.exception.FileDoesNotExistsException;
import com.al3000.cloudbase.exception.InternalServerException;
import com.al3000.cloudbase.repository.FileRepository;
import com.al3000.cloudbase.service.search.StringSearchAlgorithm;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.hibernate.internal.util.collections.CollectionHelper.listOf;

@Service
@RequiredArgsConstructor
public class FileService {
    private final FileRepository fileRepository;
    private final StringSearchAlgorithm searchService;

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

    public FileInfo move(FilePath path, FilePath target) throws DestinationAlreadyExistsException, InternalServerException {
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
            throw new DestinationAlreadyExistsException("File already exists");

        try {
            addRecursivelyFolders(target);

            for (var entry : renameList) {
                fileRepository.copyObject(entry.getFirst(), entry.getSecond());
            }
            fileRepository.removeFiles(renameList.stream()
                    .map(Pair::getFirst)
                    .collect(Collectors.toList()));

            return fileRepository.getFileInformation(target).getFileInfo();
        } catch (IOException | FileDoesNotExistsException | ServerException | InsufficientDataException |
                 ErrorResponseException | NoSuchAlgorithmException | InvalidKeyException | InvalidResponseException
                 | XmlParserException | InternalException e) {
            throw new InternalServerException(e);
        }

    }

    public Stream<FileInfo> findFiles(FilePath filePath, String query) {
        return fileRepository.getFolderContent(filePath, true)
                .filter(x -> searchService.contains(x.name(), query))
                .filter(FileFullInfo::isFile)
                .map(FileFullInfo::getFileInfo);
    }

    public InputStreamResource downloadObject(FilePath path) throws FileDoesNotExistsException, InternalServerException {
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
            throw new InternalServerException(e);
        }

    }

    void downloadFolder(FilePath path, ZipOutputStream zipOut) throws IOException, FileDoesNotExistsException, InternalServerException {
        for (var file : fileRepository.getFolderContent(path, true).toList()) {
            // skip folder markers
            if (file.isDir()) continue;

            // Read object content
            try (InputStream in = fileRepository.downloadFile(file.getFilePath())
            ) {
                // ZIP entry name should be relative to the folder
                String entryName = file.getFilePath().path().substring(path.path().length());

                zipOut.putNextEntry(new ZipEntry(entryName));

                in.transferTo(zipOut);

                zipOut.closeEntry();
            }
        }
    }

    public FileInfo createFolder(FilePath filePath) throws InternalServerException {
        return fileRepository.createFolder(filePath).getFileInfo();
    }
}
