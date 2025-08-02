package org.arkadipta.opus.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class OtpUtil {

        private static final SecureRandom RANDOM = new SecureRandom();

        public String generateOtp() {
                int otp = RANDOM.nextInt(1_000_000); // 10^6
                return String.format("%06d", otp);
        }
}
