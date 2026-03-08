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
                            .object(path.getFullPath())
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
                            .object(path.getFullPath() + file.getOriginalFilename())
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
                                        .map(x -> new DeleteObject((x.getFullPath())))
                                        .toList())
                        .build()
        ).forEach(_ -> {});
    }

    public FileFullInfo getFileInformation(FilePath filePath) throws FileDoesNotExist {
        try {
            var statObject = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(getBucketName())
                            .object(filePath.getFullPath())
                            .build()
            );
            var isDir = filePath.path().endsWith("/");
            if (isDir) {
                return new FileFullInfo(
                        filePath.username(),
                        filePath.getDirectoryPath(),
                        filePath.getDirectoryName(),
                        statObject.size(),
                        true                );
            }
            return new FileFullInfo(
                    filePath.username(),
                    filePath.getFilePath(),
                    filePath.getFileName(),
                    statObject.size(),
                    false
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
                            .object(filePath.getFullPath())
                            .stream(emptyStream, 0, -1)
                            .build()
            );
            var path = new FilePath(objectWriteResponse.object());

            return new FileFullInfo(
                    filePath.username(),
                    path.getDirectoryPath(),
                    path.getDirectoryName(),
                    0L,
                    true
            );
        } catch (Exception e) {
            throw new InternalServerException(e.getMessage());
        }
    }

    public Stream<FileFullInfo> getFolderContent(FilePath folderPath, Boolean recursive) {
        return StreamSupport.stream(minioClient.listObjects(
                        ListObjectsArgs.builder()
                                .bucket(getBucketName())
                                .prefix(folderPath.getFullPath())
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
                                path = path.substring(0, path.lastIndexOf('/')+1);
                                name = noPrefix.substring(path.length());
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
                                    isDir
                            ));
                        }
                ).flatMap(Optional::stream);
    }

    public boolean fileExist(FilePath path) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(getBucketName())
                            .object(path.getFullPath())
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
                        .object(target.getFullPath())
                        .source(
                                CopySource.builder()
                                        .bucket(getBucketName())
                                        .object(source.getFullPath())
                                        .build()
                        )
                        .build()

        );
    }
}