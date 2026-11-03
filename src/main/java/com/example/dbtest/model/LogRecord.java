package com.example.dbtest.model;

import java.util.UUID;

public record LogRecord(
    UUID traceId,
    short severity,
    String body
) {}
