package org.arkadipta.opus.util;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

public class OtpUtil {

    public static String generate6DigitCode() {
        int code = ThreadLocalRandom.current().nextInt(100000, 1_000_000);
        return String.valueOf(code);
    }

    public static LocalDateTime expiryAfterMinutes(int minutes) {
        return LocalDateTime.now().plusMinutes(minutes);
    }
}
