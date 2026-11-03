package com.example.dbtest.svc;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Random;

@Component
public class MockLogRecordProvider {

    @Value("${loader.body-size:3072}")
    private String bodySizeProp;

    private int parsedBodySize;
    private String mockLogsContent;

    @PostConstruct
    public void init() {
        try {
            String cleanProp = bodySizeProp.trim().replace("_", "").replace(",", "");
            parsedBodySize = Math.max(0, Integer.parseInt(cleanProp));
        } catch (NumberFormatException e) {
            System.err.println("Invalid body-size value: " + bodySizeProp + ", falling back to 3,072");
            parsedBodySize = 3072;
        }

        File logFile = new File("MOCK_LOGS.log");
        if (!logFile.exists()) {
            logFile = new File("src/main/resources/MOCK_LOGS.log");
        }
        if (!logFile.exists()) {
            throw new RuntimeException("MOCK_LOGS.log not found!");
        }

        try {
            this.mockLogsContent = Files.readString(logFile.toPath());
            if (this.mockLogsContent.isEmpty()) {
                throw new RuntimeException("MOCK_LOGS.log is empty!");
            }
            System.out.println("Successfully loaded MOCK_LOGS.log content.");
        } catch (IOException e) {
            throw new RuntimeException("Failed to read MOCK_LOGS.log", e);
        }
    }

    public int getParsedBodySize() {
        return parsedBodySize;
    }

    public String getMockLogBody(Random random) {
        int targetLength = parsedBodySize + random.nextInt(128);

        if (mockLogsContent == null || mockLogsContent.isEmpty()) {
            return "A".repeat(targetLength);
        }

        if (mockLogsContent.length() >= targetLength) {
            int maxOffset = mockLogsContent.length() - targetLength;
            int startOffset = random.nextInt(maxOffset + 1);
            return mockLogsContent.substring(startOffset, startOffset + targetLength);
        } else {
            StringBuilder sb = new StringBuilder();
            while (sb.length() < targetLength) {
                int remaining = targetLength - sb.length();
                if (remaining >= mockLogsContent.length()) {
                    sb.append(mockLogsContent);
                } else {
                    int maxOffset = mockLogsContent.length() - remaining;
                    int startOffset = random.nextInt(maxOffset + 1);
                    sb.append(mockLogsContent.substring(startOffset, startOffset + remaining));
                }
            }
            return sb.toString();
        }
    }
}
