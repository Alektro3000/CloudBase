package com.al3000.cloudbase.bdd.steps;

import com.al3000.cloudbase.exception.DestinationAlreadyExist;
import com.al3000.cloudbase.exception.FileDoesNotExist;
import com.al3000.cloudbase.exception.InternalServerException;
import com.al3000.cloudbase.service.FileService;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class FileServiceSteps extends BaseStepDefinitions {

    @Autowired
    protected FileService fileService;

    @Before
    public void resetState() {
        reset(fileService);
    }

    @Given("the file service will return the following folder entries:")
    public void theFileServiceWillReturnTheFollowingFolderEntries(DataTable dataTable) {
        when(fileService.getFolderFiles(any()))
                .thenReturn(toFileInfoList(dataTable).stream());
    }

    @Given("the file service will create the folder result:")
    public void theFileServiceWillCreateTheFolderResult(DataTable dataTable) throws Exception {
        when(fileService.createFolder(any()))
                .thenReturn(singleFileInfo(dataTable));
    }

    @Given("the file service will fail folder creation with internal error {string}")
    public void theFileServiceWillFailFolderCreationWithInternalError(String message) throws Exception {
        when(fileService.createFolder(any()))
                .thenThrow(new InternalServerException(message));
    }

    @Given("the file service will accept uploaded files")
    public void theFileServiceWillAcceptUploadedFiles() {
    }

    @Given("the file service will fail upload with internal error {string}")
    public void theFileServiceWillFailUploadWithInternalError(String message) throws Exception {
        doThrow(new InternalServerException(message))
                .when(fileService).uploadFile(any(), any());
    }

    @Given("the file service will return the following search results:")
    public void theFileServiceWillReturnTheFollowingSearchResults(DataTable dataTable) {
        when(fileService.findFiles(any(), any()))
                .thenReturn(toFileInfoList(dataTable).stream());
    }

    @Given("the file service will move the resource result:")
    public void theFileServiceWillMoveTheResourceResult(DataTable dataTable) throws Exception {
        when(fileService.move(any(), any()))
                .thenReturn(singleFileInfo(dataTable));
    }

    @Given("the file service will fail move with destination conflict {string}")
    public void theFileServiceWillFailMoveWithDestinationConflict(String message) throws Exception {
        when(fileService.move(any(), any()))
                .thenThrow(new DestinationAlreadyExist(message));
    }

    @Given("the file service will accept deletions")
    public void theFileServiceWillAcceptDeletions() {
    }

    @Given("the file service will return downloadable content {string}")
    public void theFileServiceWillReturnDownloadableContent(String body) throws Exception {
        when(fileService.downloadObject(any()))
                .thenReturn(new InputStreamResource(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))));
    }

    @Given("the file service will fail download with missing file {string}")
    public void theFileServiceWillFailDownloadWithMissingFile(String message) throws Exception {
        when(fileService.downloadObject(any()))
                .thenThrow(new FileDoesNotExist(message));
    }

    @And("the file service should have been asked to list folder {string} for user {string}")
    public void theFileServiceShouldHaveBeenAskedToListFolderForUser(String path, String username) {
        verify(fileService).getFolderFiles(argThat(filePath ->
                Objects.equals(filePath.username(), username) &&
                        Objects.equals(filePath.path(), path)));
    }

    @And("the file service should have been asked to create folder {string} for user {string}")
    public void theFileServiceShouldHaveBeenAskedToCreateFolderForUser(String path, String username) throws Exception {
        verify(fileService).createFolder(argThat(filePath ->
                Objects.equals(filePath.username(), username) &&
                        Objects.equals(filePath.path(), path)));
    }

    @And("the file service should have been asked to upload file {string} to {string} for user {string}")
    public void theFileServiceShouldHaveBeenAskedToUploadFileToForUser(String filename, String path, String username) throws Exception {
        verify(fileService).uploadFile(
                argThat(file -> Objects.equals(file.getOriginalFilename(), filename)),
                argThat(filePath -> Objects.equals(filePath.username(), username) && Objects.equals(filePath.path(), path))
        );
    }

    @And("the file service should have been asked to remove path {string} for user {string}")
    public void theFileServiceShouldHaveBeenAskedToRemovePathForUser(String path, String username) {
        verify(fileService).removeFile(argThat(filePath ->
                Objects.equals(filePath.username(), username) &&
                        Objects.equals(filePath.path(), path)));
    }

    @And("the file service should have been asked to search for {string} for user {string}")
    public void theFileServiceShouldHaveBeenAskedToSearchForForUser(String query, String username) {
        verify(fileService).findFiles(
                argThat(filePath -> Objects.equals(filePath.username(), username) && Objects.equals(filePath.path(), "")),
                eq(query)
        );
    }

    @And("the file service should have been asked to move from {string} to {string} for user {string}")
    public void theFileServiceShouldHaveBeenAskedToMoveFromToForUser(String from, String to, String username) throws Exception {
        verify(fileService).move(
                argThat(filePath -> Objects.equals(filePath.username(), username) && Objects.equals(filePath.path(), from)),
                argThat(filePath -> Objects.equals(filePath.username(), username) && Objects.equals(filePath.path(), to))
        );
    }

    @And("the file service should have been asked to download path {string} for user {string}")
    public void theFileServiceShouldHaveBeenAskedToDownloadPathForUser(String path, String username) throws Exception {
        verify(fileService).downloadObject(argThat(filePath ->
                Objects.equals(filePath.username(), username) &&
                        Objects.equals(filePath.path(), path)));
    }
}
