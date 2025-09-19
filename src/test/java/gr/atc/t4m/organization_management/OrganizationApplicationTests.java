package gr.atc.t4m.organization_management;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.junit.jupiter.api.Assertions;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import gr.atc.t4m.organization_management.config.KafkaTestConfig;
@SpringBootTest
@Import(KafkaTestConfig.class)
@ActiveProfiles("test")

class OrganizationApplicationTests {
	    @MockBean
    private RestTemplate restTemplate;

	@Test
	void contextLoads() {
		Assertions.assertNotNull(ApplicationContext.class);

	}

}
