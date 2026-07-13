package com.example.demo.file.record;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.file.bucket.BucketComponent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FileRecordRepositoryTest {

  private final BucketComponent bucketComponent = mock(BucketComponent.class);
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  private final FileRecordRepository repository =
      new FileRecordRepository(bucketComponent, objectMapper);

  @Test
  void save_uploadsJsonRecordToBucket() {
    var record =
        new FileRecord(
            UUID.randomUUID(), "photo.jpg", "student@example.com", "files/key.jpg", Instant.now());

    repository.save(record);

    verify(bucketComponent).upload(any(File.class), eq("records/" + record.id() + ".json"));
  }

  @Test
  void findAll_downloadsAndDeserializesAllRecords() throws Exception {
    var record =
        new FileRecord(
            UUID.randomUUID(), "photo.jpg", "student@example.com", "files/key.jpg", Instant.now());
    var jsonFile = File.createTempFile("record-", ".json");
    Files.writeString(jsonFile.toPath(), objectMapper.writeValueAsString(record));

    when(bucketComponent.list("records/")).thenReturn(List.of("records/" + record.id() + ".json"));
    when(bucketComponent.download("records/" + record.id() + ".json")).thenReturn(jsonFile);

    var result = repository.findAll();

    assertThat(result).containsExactly(record);
  }
}
