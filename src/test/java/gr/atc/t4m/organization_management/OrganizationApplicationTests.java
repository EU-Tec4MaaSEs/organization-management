package gr.atc.t4m.organization_management;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import gr.atc.t4m.organization_management.config.KafkaTestConfig;
@SpringBootTest
@Import(KafkaTestConfig.class)
@ActiveProfiles("test")

class OrganizationApplicationTests {

	@Test
	void contextLoads() {
		Assertions.assertNotNull(ApplicationContext.class);

	}

}
