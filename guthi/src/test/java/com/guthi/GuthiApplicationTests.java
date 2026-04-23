package com.guthi;

import com.guthi.support.MongoSpringBootTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class GuthiApplicationTests extends MongoSpringBootTestSupport {

    @Test
    void contextLoads() {}
}
