package com.al3000.CloudBase.service;

import com.al3000.CloudBase.dto.FileInfo;
import com.al3000.CloudBase.dto.FilePath;
import com.al3000.CloudBase.exception.DestinationAlreadyExist;
import com.al3000.CloudBase.exception.FileDoesNotExist;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.Exceptions;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class FileRepository {
    MinioClient minioClient;
    @Value("${minio.bucket}")
    String userBucketName;

    @Autowired
    FileRepository(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public String getBucketName() {
        return userBucketName;
    }

    public void uploadFile(MultipartFile file, FilePath path) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(getBucketName())
                            .object(path.getFilePath() + file.getOriginalFilename())
                            .stream(
                                    file.getInputStream(),
                                    file.getSize(),
                                    -1
                            )
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (IOException | ServerException | InsufficientDataException | ErrorResponseException |
                 NoSuchAlgorithmException | InvalidKeyException | InvalidResponseException | XmlParserException |
                 InternalException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeFile(FilePath path) {
        var removeList = createFileInfoStream(path, true)
                .map(x ->
                        new DeleteObject(
                                path.getPrefix() + x.getPath() + x.getName()
                        ))
                .toList();

        minioClient.removeObjects(
                RemoveObjectsArgs.builder()
                        .bucket(getBucketName())
                        .objects(removeList)
                        .build()
        ).forEach(x -> {
        });
    }

    public FileInfo moveFile(FilePath path, FilePath target) {
        var renameList = createFileInfoStream(path, true)
                .map(object -> {
                            var sourcePath = path.getPrefix() + object.getFullPath();
                            var targetPath = sourcePath.replace(path.getFilePath(), target.getFilePath());
                            return new AbstractMap.SimpleEntry<String, String>(sourcePath, targetPath);
                        }
                ).toList();

        if (renameList.stream().anyMatch(x -> fileExist(x.getValue())))
            throw new DestinationAlreadyExist("File already exists");

        try {

            for (var entry : renameList) {
                copyObject(entry.getKey(), entry.getValue());
            }

            minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(getBucketName())
                            .objects(renameList.stream()
                                    .map(x -> new DeleteObject(x.getKey()))
                                    .collect(Collectors.toList()))
                            .build()
            ).forEach(x -> {
            });
            return getFileInformation(target);
        } catch (Exception e) {
            throw Exceptions.propagate(e);
        }

    }

    public Stream<FileInfo> findFiles(FilePath filePath, String query) {
        return createFileInfoStream(filePath, true)
                .filter(x -> x.getName().contains(query))
                .filter(x -> !"".equalsIgnoreCase(x.getName()));
    }

    public FileInfo getFileInformation(FilePath filePath) {
        try {
            var statObject = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(getBucketName())
                            .object(filePath.getFilePath())
                            .build()
            );
            return new FileInfo(
                    filePath.getPath(),
                    statObject.object(),
                    statObject.size(),
                    statObject.contentType()
            );
        } catch (Exception e) {
            throw new FileDoesNotExist(e.getMessage());
        }
    }

    public FileInfo createFolder(FilePath filePath) {
        byte[] empty = new byte[0];
        ByteArrayInputStream emptyStream = new ByteArrayInputStream(empty);
        try {
            var objectWriteResponse = minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(getBucketName())
                            .object(filePath.getFilePath())
                            .stream(emptyStream, 0, -1)
                            .build()
            );
            return new FileInfo(
                    filePath.getPath(),
                    objectWriteResponse.object(),
                    0L,
                    "Directory"
            );
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public Stream<FileInfo> getFolderContent(FilePath filePath) {
        return createFileInfoStream(filePath, false)
                .filter(x -> !"".equalsIgnoreCase(x.getName()));
    }

    public Stream<FileInfo> createFileInfoStream(FilePath filePath, Boolean recursive) {
        return StreamSupport.stream(minioClient.listObjects(
                        ListObjectsArgs.builder()
                                .bucket(getBucketName())
                                .prefix(filePath.getFilePath())
                                .recursive(recursive)
                                .build()
                ).spliterator(), false)
                .map((file) -> {
                            try {
                                var object = file.get();
                                var objectName = object.objectName();
                                return Optional.of(new FileInfo(
                                        filePath.getPath(),
                                        objectName.substring(filePath.getFilePath().length()),
                                        object.size(),
                                        objectName.endsWith("/") ? "DIRECTORY" : "FILE"
                                ));
                            } catch (Exception e) {
                                return Optional.<FileInfo>empty();
                            }
                        }
                ).flatMap(Optional::stream);
    }

    public Boolean fileExist(String path) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(getBucketName())
                            .object(path)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public ObjectWriteResponse copyObject(String source, String target) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        return minioClient.copyObject(
                CopyObjectArgs.builder()
                        .bucket(getBucketName())
                        .object(target)
                        .source(
                                CopySource.builder()
                                        .bucket(getBucketName())
                                        .object(source)
                                        .build()
                        )
                        .build()

        );
    }

    public void downloadFolder(FilePath path, ZipOutputStream zipOut) {
        Iterable<Result<Item>> objects = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(getBucketName())
                        .prefix(path.getFilePath())      // e.g. "user123/"
                        .recursive(true)
                        .build()
        );

        for (Result<Item> result : objects) {
            try {
                Item item = result.get();

                String objectName = item.objectName();
                // skip folder markers
                if (item.isDir() || objectName.endsWith("/")) continue;

                // Read object content
                try (InputStream in = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(getBucketName())
                                .object(objectName)
                                .build()
                )) {
                    // ZIP entry name should be relative to the folder
                    String entryName = objectName.substring(path.getFilePath().length());

                    zipOut.putNextEntry(new ZipEntry(entryName));

                    in.transferTo(zipOut);

                    zipOut.closeEntry();
                }
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }

    }

    public InputStream downloadFile(FilePath path) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(getBucketName())
                            .object(path.getFilePath())
                            .build()
            );
        } catch (Exception e) {
            throw new FileDoesNotExist(e.getMessage());
        }
    }
}