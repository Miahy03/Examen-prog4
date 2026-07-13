package com.example.demo.endpoint.rest.controller.file;

import com.example.demo.endpoint.rest.model.file.FileResponse;
import com.example.demo.file.bucket.BucketComponent;
import com.example.demo.file.image.ImageGrayscaleConverter;
import com.example.demo.file.record.FileRecord;
import com.example.demo.file.record.FileRecordRepository;
import com.example.demo.file.zip.FileTyper;
import com.example.demo.mail.Email;
import com.example.demo.mail.Mailer;
import jakarta.mail.internet.InternetAddress;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@AllArgsConstructor
public class FileController {

  private static final String BUCKET_PREFIX = "files/";
  private static final Duration LINK_EXPIRATION = Duration.ofDays(7);

  private final BucketComponent bucketComponent;
  private final FileTyper fileTyper;
  private final ImageGrayscaleConverter imageGrayscaleConverter;
  private final Mailer mailer;
  private final FileRecordRepository fileRecordRepository;

  @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<FileResponse> uploadFile(
      @RequestParam("file") MultipartFile file, @RequestParam("email") String email)
      throws IOException {
    var id = UUID.randomUUID();
    var fileToUpload = toTempFile(file);
    var mediaType = fileTyper.apply(fileToUpload);

    if (MediaType.IMAGE_JPEG.isCompatibleWith(mediaType)) {
      fileToUpload = imageGrayscaleConverter.toGrayscale(fileToUpload);
    }

    var bucketKey = BUCKET_PREFIX + id + "-" + file.getOriginalFilename();
    bucketComponent.upload(fileToUpload, bucketKey);
    var downloadLink = bucketComponent.presign(bucketKey, LINK_EXPIRATION).toString();

    var fileRecord =
        fileRecordRepository.save(
            new FileRecord(id, file.getOriginalFilename(), email, bucketKey, Instant.now()));

    sendUploadConfirmationEmail(email, fileRecord, downloadLink);

    return ResponseEntity.status(HttpStatus.CREATED).body(toFileResponse(fileRecord, downloadLink));
  }

  @GetMapping("/files")
  public ResponseEntity<List<FileResponse>> findAllFiles() {
    var responses =
        fileRecordRepository.findAll().stream()
            .map(
                fileRecord ->
                    toFileResponse(
                        fileRecord,
                        bucketComponent
                            .presign(fileRecord.bucketKey(), LINK_EXPIRATION)
                            .toString()))
            .toList();
    return ResponseEntity.ok(responses);
  }

  @SneakyThrows
  private File toTempFile(MultipartFile file) {
    var tempFile = File.createTempFile("upload-", "-" + file.getOriginalFilename());
    file.transferTo(tempFile);
    return tempFile;
  }

  @SneakyThrows
  private void sendUploadConfirmationEmail(
      String email, FileRecord fileRecord, String downloadLink) {
    mailer.accept(
        new Email(
            new InternetAddress(email),
            List.of(),
            List.of(),
            "Your file \"" + fileRecord.fileName() + "\" has been uploaded",
            "<p>Hello,</p>"
                + "<p>Your file <b>"
                + fileRecord.fileName()
                + "</b> was uploaded successfully.</p>"
                + "<p>You can download it here: <a href=\""
                + downloadLink
                + "\">"
                + downloadLink
                + "</a></p>",
            List.of()));
  }

  private FileResponse toFileResponse(FileRecord fileRecord, String downloadLink) {
    return new FileResponse(
        fileRecord.id(),
        fileRecord.fileName(),
        fileRecord.email(),
        fileRecord.createdAt(),
        downloadLink);
  }
}
