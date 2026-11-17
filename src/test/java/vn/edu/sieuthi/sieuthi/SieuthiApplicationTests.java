package vn.edu.sieuthi.sieuthi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
	"spring.sql.init.mode=never",
	"app.auth.manager.username=",
	"app.auth.manager.password="
})
class SieuthiApplicationTests {

	@Test
	void contextLoads() {
	}

}
