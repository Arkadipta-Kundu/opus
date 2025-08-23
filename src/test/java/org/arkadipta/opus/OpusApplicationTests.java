package org.arkadipta.opus;

import org.arkadipta.opus.config.TestEmailConfig;
import org.arkadipta.opus.config.TestRedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration," +
                "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration"
})
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Import({TestEmailConfig.class, TestRedisConfig.class})
class OpusApplicationTests {

    @Test
    void contextLoads() {
        // This test verifies that the Spring application context loads successfully
        // with test configuration, excluding Redis and Mail auto-configuration
    }

}
