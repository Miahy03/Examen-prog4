package com.example.demo.file.record;

import java.time.Instant;
import java.util.UUID;

public record FileRecord(
    UUID id, String fileName, String email, String bucketKey, Instant createdAt) {}
