package com.example.demo.endpoint.rest.model.file;

import java.time.Instant;
import java.util.UUID;

public record FileResponse(
    UUID id, String fileName, String email, Instant createdAt, String downloadLink) {}
