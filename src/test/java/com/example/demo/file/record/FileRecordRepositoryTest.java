package com.example.demo.file.record;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FileRecordRepositoryTest {

  private final FileRecordRepository repository = new FileRecordRepository();

  @Test
  void save_thenFindAll_returnsSavedRecord() {
    var record =
        new FileRecord(
            UUID.randomUUID(), "photo.jpg", "student@example.com", "files/key.jpg", Instant.now());

    repository.save(record);

    assertThat(repository.findAll()).containsExactly(record);
  }

  @Test
  void findAll_isEmptyByDefault() {
    assertThat(repository.findAll()).isEmpty();
  }
}
