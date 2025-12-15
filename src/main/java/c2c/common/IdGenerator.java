package c2c.common;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class IdGenerator {
    private static final AtomicLong SEQ = new AtomicLong(1);

    private IdGenerator() {
    }

    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    public static String nextNumeric() {
        return String.valueOf(SEQ.getAndIncrement());
    }
}
