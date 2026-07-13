package com.example.demo.file.record;

import com.example.demo.file.bucket.BucketComponent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class FileRecordRepository {

  private static final String RECORDS_PREFIX = "records/";

  private final BucketComponent bucketComponent;
  private final ObjectMapper objectMapper;

  @SneakyThrows
  public FileRecord save(FileRecord fileRecord) {
    var json = objectMapper.writeValueAsString(fileRecord);
    var tempFile = File.createTempFile("record-", ".json");
    Files.writeString(tempFile.toPath(), json);
    bucketComponent.upload(tempFile, recordKey(fileRecord.id()));
    return fileRecord;
  }

  public List<FileRecord> findAll() {
    return bucketComponent.list(RECORDS_PREFIX).stream().map(this::readRecord).toList();
  }

  @SneakyThrows
  private FileRecord readRecord(String bucketKey) {
    var downloaded = bucketComponent.download(bucketKey);
    return objectMapper.readValue(downloaded, FileRecord.class);
  }

  private String recordKey(UUID id) {
    return RECORDS_PREFIX + id + ".json";
  }
}
