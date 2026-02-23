package com.al3000.cloudbase.repository;

import com.al3000.cloudbase.dto.FileFullInfo;
import com.al3000.cloudbase.dto.FilePath;
import com.al3000.cloudbase.exception.FileDoesNotExist;
import com.al3000.cloudbase.exception.InternalServerException;
import io.minio.*;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


@Repository
public class FileRepository {

    private final MinioClient minioClient;
    private final String userBucketName;

    public FileRepository(
            MinioClient minioClient,
            @Value("${minio.bucket}") String userBucketName
    ) {
        this.minioClient = minioClient;
        this.userBucketName = userBucketName;
    }

    private String getBucketName() {
        return userBucketName;
    }

    public InputStream downloadFile(FilePath path) throws FileDoesNotExist {
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

    public void uploadFile(MultipartFile file, FilePath path) throws InternalServerException {
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
        } catch (Exception e) {
            throw new InternalServerException(e.getMessage());
        }
    }

    public void removeFiles(List<FilePath> deleteObjects) {
        minioClient.removeObjects(
                RemoveObjectsArgs.builder()
                        .bucket(getBucketName())
                        .objects(
                                deleteObjects.stream()
                                        .map(x -> new DeleteObject((x.getFilePath())))
                                        .toList())
                        .build()
        ).forEach(x -> {
        });
    }

    public FileFullInfo getFileInformation(FilePath filePath) throws FileDoesNotExist {
        try {
            var statObject = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(getBucketName())
                            .object(filePath.getFilePath())
                            .build()
            );
            return new FileFullInfo(
                    filePath.username(),
                    filePath.path(),
                    statObject.object(),
                    statObject.size(),
                    filePath.path().endsWith("/") ? "DIRECTORY" : "FILE"
            );
        } catch (Exception e) {
            throw new FileDoesNotExist(e.getMessage());
        }
    }

    public FileFullInfo createFolder(FilePath filePath) throws InternalServerException {
        byte[] empty = new byte[0];
        try (var emptyStream = new ByteArrayInputStream(empty)) {
            var objectWriteResponse = minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(getBucketName())
                            .object(filePath.getFilePath())
                            .stream(emptyStream, 0, -1)
                            .build()
            );
            return new FileFullInfo(
                    filePath.username(),
                    filePath.path(),
                    objectWriteResponse.object(),
                    0L,
                    "Directory"
            );
        } catch (Exception e) {
            throw new InternalServerException(e.getMessage());
        }
    }

    public Stream<FileFullInfo> getFolderContent(FilePath folderPath, Boolean recursive) {
        return StreamSupport.stream(minioClient.listObjects(
                        ListObjectsArgs.builder()
                                .bucket(getBucketName())
                                .prefix(folderPath.getFilePath())
                                .recursive(recursive)
                                .build()
                ).spliterator(), false)
                .map((file) -> {
                            Item object;
                            try {
                                object = file.get();
                            } catch (Exception e) {
                                return Optional.<FileFullInfo>empty();
                            }
                            var objectName = object.objectName();

                            var isDir = objectName.endsWith("/");

                            var noPrefix = objectName.substring(folderPath.getPrefix().length());
                            if (isDir) {
                                noPrefix = noPrefix.substring(0, noPrefix.length() - 1);
                            }

                            var path = noPrefix;
                            String name;
                            if (path.contains("/")) {
                                path = path.substring(0, path.lastIndexOf('/'));
                                name = noPrefix.substring(path.length() + 1);
                            } else {
                                //Case of empty path
                                name = path;
                                path = "";
                            }

                            return Optional.of(new FileFullInfo(
                                    folderPath.username(),
                                    path,
                                    name,
                                    object.size(),
                                    isDir ? "DIRECTORY" : "FILE"
                            ));
                        }
                ).flatMap(Optional::stream);
    }

    public boolean fileExist(FilePath path) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(getBucketName())
                            .object(path.getFilePath())
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void copyObject(FilePath source, FilePath target) throws Exception {
        minioClient.copyObject(
                CopyObjectArgs.builder()
                        .bucket(getBucketName())
                        .object(target.getFilePath())
                        .source(
                                CopySource.builder()
                                        .bucket(getBucketName())
                                        .object(source.getFilePath())
                                        .build()
                        )
                        .build()

        );
    }
}