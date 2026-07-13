package com.example.demo.endpoint.rest.controller.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.file.bucket.BucketComponent;
import com.example.demo.file.hash.FileHash;
import com.example.demo.file.hash.FileHashAlgorithm;
import com.example.demo.file.image.ImageGrayscaleConverter;
import com.example.demo.file.record.FileRecord;
import com.example.demo.file.record.FileRecordRepository;
import com.example.demo.file.zip.FileTyper;
import com.example.demo.mail.Email;
import com.example.demo.mail.Mailer;
import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class FileControllerTest {

  private final BucketComponent bucketComponent = mock(BucketComponent.class);
  private final FileTyper fileTyper = mock(FileTyper.class);
  private final ImageGrayscaleConverter imageGrayscaleConverter =
      mock(ImageGrayscaleConverter.class);
  private final Mailer mailer = mock(Mailer.class);
  private final FileRecordRepository fileRecordRepository = mock(FileRecordRepository.class);

  private FileController fileController;

  @BeforeEach
  void setUp() {
    fileController =
        new FileController(
            bucketComponent, fileTyper, imageGrayscaleConverter, mailer, fileRecordRepository);
  }

  @Test
  void uploadFile_convertsJpegToGrayscaleAndSendsEmail() throws Exception {
    var multipartFile =
        new MockMultipartFile("file", "photo.jpg", "image/jpeg", "fake-jpeg-content".getBytes());
    var grayscaleFile = File.createTempFile("grayscale-", ".jpg");
    var presignedUrl = new URL("https://bucket.s3.amazonaws.com/files/photo.jpg");

    when(fileTyper.apply(any(File.class))).thenReturn(MediaType.IMAGE_JPEG);
    when(imageGrayscaleConverter.toGrayscale(any(File.class))).thenReturn(grayscaleFile);
    when(bucketComponent.upload(any(File.class), any(String.class)))
        .thenReturn(new FileHash(FileHashAlgorithm.SHA256, "hash"));
    when(bucketComponent.presign(any(String.class), any(Duration.class))).thenReturn(presignedUrl);
    when(fileRecordRepository.save(any(FileRecord.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response = fileController.uploadFile(multipartFile, "student@example.com");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().fileName()).isEqualTo("photo.jpg");
    assertThat(response.getBody().email()).isEqualTo("student@example.com");
    assertThat(response.getBody().downloadLink()).isEqualTo(presignedUrl.toString());

    verify(imageGrayscaleConverter).toGrayscale(any(File.class));
    verify(bucketComponent).upload(eq(grayscaleFile), any(String.class));
    verify(mailer).accept(any(Email.class));
  }

  @Test
  void uploadFile_doesNotConvertNonJpegFiles() throws Exception {
    var multipartFile =
        new MockMultipartFile(
            "file", "document.pdf", "application/pdf", "fake-pdf-content".getBytes());
    var presignedUrl = new URL("https://bucket.s3.amazonaws.com/files/document.pdf");

    when(fileTyper.apply(any(File.class))).thenReturn(MediaType.APPLICATION_PDF);
    when(bucketComponent.upload(any(File.class), any(String.class)))
        .thenReturn(new FileHash(FileHashAlgorithm.SHA256, "hash"));
    when(bucketComponent.presign(any(String.class), any(Duration.class))).thenReturn(presignedUrl);
    when(fileRecordRepository.save(any(FileRecord.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response = fileController.uploadFile(multipartFile, "student@example.com");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    verify(imageGrayscaleConverter, never()).toGrayscale(any(File.class));
  }

  @Test
  void findAllFiles_returnsAllStoredRecordsWithFreshLinks() throws Exception {
    var record =
        new FileRecord(
            UUID.randomUUID(), "photo.jpg", "student@example.com", "files/key.jpg", Instant.now());
    var presignedUrl = new URL("https://bucket.s3.amazonaws.com/files/key.jpg");

    when(fileRecordRepository.findAll()).thenReturn(List.of(record));
    when(bucketComponent.presign(eq(record.bucketKey()), any(Duration.class)))
        .thenReturn(presignedUrl);

    var response = fileController.findAllFiles();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
    assertThat(response.getBody().get(0).downloadLink()).isEqualTo(presignedUrl.toString());
  }
}
