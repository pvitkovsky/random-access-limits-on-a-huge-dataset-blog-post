package com.example.dbtest.util;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;

public class ULIDUtil {
    public static UUID generateRandomUlid(Random random) {

        long nowMs = Instant.now().toEpochMilli();
        long sixMonthsAgoMs = nowMs - Duration.ofDays(180).toMillis();
        long randomTimestamp = sixMonthsAgoMs + (long) (random.nextDouble() * (nowMs - sixMonthsAgoMs));

        long msb = (randomTimestamp << 16) | (random.nextLong() & 0xFFFFL);
        long lsb = random.nextLong();
        return new UUID(msb, lsb);
    }


}
