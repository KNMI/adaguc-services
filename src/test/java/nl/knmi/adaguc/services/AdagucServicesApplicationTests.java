package nl.knmi.adaguc.services;


import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AdagucServicesApplicationTests {

	/** The Spring web application context. */
	@Resource
	private WebApplicationContext webApplicationContext;

	@Before
	public void setUp() {

		MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}


	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void TestThis(){

	}


}
