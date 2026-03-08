// java
package com.al3000.cloudbase.repository;

import com.al3000.cloudbase.dto.FilePath;
import com.al3000.cloudbase.exception.FileDoesNotExist;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.internal.util.collections.CollectionHelper.listOf;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class FileRepositoryMinioIntegrationTest {
    static final String ACCESS_KEY = "minioadmin";
    static final String SECRET_KEY = "minioadmin";
    static final String BUCKET = "test-bucket";

    static DockerImageName MINIO_IMAGE = DockerImageName.parse("minio/minio:latest");

    // запуск контейнера
    static final GenericContainer<?> minio = new GenericContainer<>(MINIO_IMAGE)
            .withEnv("MINIO_ROOT_USER", ACCESS_KEY)
            .withEnv("MINIO_ROOT_PASSWORD", SECRET_KEY)
            .withCommand("server", "/data")
            .withExposedPorts(9000);

    static {
        minio.start();
    }

    MinioClient minioClient;
    FileRepository fileRepository;

    final String username = "alice";
    byte[] payload = "hello from integration".getBytes(StandardCharsets.UTF_8);

    @BeforeEach
    void setupMinioClient() {
        String endpoint = "http://" + minio.getHost() + ":" + minio.getMappedPort(9000);
        minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(ACCESS_KEY, SECRET_KEY)
                .build();

        // создаём бакет если не существует
        try {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
        } catch (Exception ignored) {
            // может уже быть создан
        }
        fileRepository = new FileRepository(minioClient, BUCKET);
    }

    @Test
    void uploadFile_and_download() throws Exception {
        // Arrange
        var multipart = new MockMultipartFile("file", "hello.txt", "text/plain", payload);

        // Путь передаётся в FileService так, как в приложении: в примере — оставляем "a/"
        FilePath path = new FilePath(username, "a/");

        // Act
        fileRepository.uploadFile(multipart, path);

        // Assert — формирование ключа зависит от реализации FileRepository.
        // В этом примере ожидаем ключ: "user-{username}/{path}{filename}" -> "alice/a/hello.txt"
        String expectedObject = "user-" + username + "/" + path.path() + multipart.getOriginalFilename();

        try (InputStream is = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(BUCKET)
                        .object(expectedObject)
                        .build());
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            is.transferTo(baos);
            assertThat(baos.toByteArray()).isEqualTo(payload);
        }
    }

    @Test
    void uploadFile_and_downloadFile() throws Exception {
        // Arrange
        var multipart = new MockMultipartFile("file", "hello.txt", "text/plain", payload);

        // Путь передаётся в FileService так, как в приложении: в примере — оставляем "a/"
        FilePath path = new FilePath(username, "a/");

        fileRepository.uploadFile(multipart, path);

        FilePath EndPath = new FilePath(username, "a/hello.txt");

        //Act
        try (InputStream is = fileRepository.downloadFile(EndPath);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            //Assert
            is.transferTo(baos);
            assertThat(baos.toByteArray()).isEqualTo(payload);
            assertThat(fileRepository.fileExist(EndPath)).isTrue();
        }
    }

    @Test
    void downloadFile_whenFileDoesNotExist_throwsFileDoesNotExist() {
        // Arrange
        FilePath EndPath = new FilePath(username, "a/hik.txt");

        //Act & Assert
        assertThatThrownBy(() -> fileRepository.downloadFile(EndPath)).isInstanceOf(FileDoesNotExist.class);

    }

    @Test
    void uploadFile_and_removeFile() throws Exception {
        // Arrange
        var multipart = new MockMultipartFile("file", "hello.txt", "text/plain", payload);

        // Путь передаётся в FileService так, как в приложении: в примере — оставляем "a/"
        FilePath path = new FilePath(username, "a/");

        fileRepository.uploadFile(multipart, path);

        FilePath EndPath = new FilePath(username, "a/hello.txt");

        //Act
        fileRepository.removeFiles(listOf(EndPath));

        //Assert
        assertThat(fileRepository.fileExist(EndPath)).isFalse();
    }


    @Test
    void uploadFile_and_getFileInformation() throws Exception {
        // Arrange
        var multipart = new MockMultipartFile("file", "hello.txt", "text/plain", payload);

        // Путь передаётся в FileService так, как в приложении: в примере — оставляем "a/"
        FilePath path = new FilePath(username, "a/");

        fileRepository.uploadFile(multipart, path);

        FilePath EndPath = new FilePath(username, "a/hello.txt");

        //Act
        var result = fileRepository.getFileInformation(EndPath);

        //Assert
        assertThat(result.isFile()).isTrue();
        assertThat(result.getFilePath()).isEqualTo(EndPath);
        assertThat(result.user()).isEqualTo(username);
        assertThat(result.path()).isEqualTo(path.path());
        assertThat(result.name()).isEqualTo("hello.txt");
        assertThat(result.size()).isEqualTo(payload.length);
    }

    @Test
    void createFolder_and_getFolderInformation() throws Exception {
        // Arrange
        FilePath path = new FilePath(username, "a/");

        //Act
        var returnFolder = fileRepository.createFolder(path);

        var result = fileRepository.getFileInformation(path);

        //Assert
        assertThat(result).isEqualTo(returnFolder);
    }


    @Test
    void getFileInformation_throwsFileDoesNotExist()  {
        // Arrange
        FilePath EndPath = new FilePath(username, "a/hi.txt");

        //Act & Assert
        assertThatThrownBy(() -> fileRepository.getFileInformation(EndPath)).isInstanceOf(FileDoesNotExist.class);
    }

    @Test
    void copyFile() throws Exception {
        // Arrange
        var multipart = new MockMultipartFile("file", "hello.txt", "text/plain", payload);

        // Путь передаётся в FileService так, как в приложении: в примере — оставляем "a/"
        FilePath path = new FilePath(username, "a/");

        fileRepository.uploadFile(multipart, path);
        FilePath EndPath = new FilePath(username, "a/hello.txt");
        FilePath TargetPath = new FilePath(username, "a/hi1.txt");

        //Act
        fileRepository.copyObject(EndPath, TargetPath);

        //Assert
        assertThat(fileRepository.fileExist(TargetPath)).isTrue();
    }

}