package gr.atc.t4m.organization_management;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.springframework.context.ApplicationContext;
@SpringBootTest

class OrganizationApplicationTests {

	@Test
	void contextLoads() {
		Assertions.assertNotNull(ApplicationContext.class);

	}

}
