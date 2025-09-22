package gr.atc.t4m.organization_management;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;

import gr.atc.t4m.organization_management.config.KafkaTestConfig;
@SpringBootTest
@Import(KafkaTestConfig.class)
@ActiveProfiles("test")

class OrganizationApplicationTests {
	    @MockitoBean
    private RestTemplate restTemplate;

	@Test
	void contextLoads() {
		Assertions.assertNotNull(ApplicationContext.class);

	}

}
