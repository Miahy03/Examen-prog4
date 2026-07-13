package com.example.demo.file.record;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class FileRecordRepository {

  private final Map<UUID, FileRecord> records = new ConcurrentHashMap<>();

  public FileRecord save(FileRecord fileRecord) {
    records.put(fileRecord.id(), fileRecord);
    return fileRecord;
  }

  public List<FileRecord> findAll() {
    return List.copyOf(records.values());
  }
}
