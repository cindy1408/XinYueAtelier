package com.xinyue.atelier;

import com.xinyue.atelier.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@Import(TestConfig.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class AtelierApplicationTests {

	@Test
	void contextLoads() {
	}

}
