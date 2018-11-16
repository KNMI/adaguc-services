package nl.knmi.adaguc.tools;


import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.UUID;

import javax.annotation.Resource;

import org.bouncycastle.operator.OperatorCreationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import nl.knmi.adaguc.security.CertificateVerificationException;
import nl.knmi.adaguc.security.PemX509Tools;
import nl.knmi.adaguc.security.PemX509Tools.X509Info;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PemX509ToolsTests {

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
	public void TestCACSRFlow() throws IOException, InvalidKeyException, NoSuchAlgorithmException, CertificateException, OperatorCreationException, CertificateVerificationException{
		
		String clientCN = "ClientCert[" + UUID.randomUUID().toString()+"]";
		String testBaseDir = "/tmp/adaguc-services-test/";
		String trustRootsDir = testBaseDir + "trustedcertificates/";
		String clientCertDir = testBaseDir + "signedcerts/";
		String clientCertLocation = clientCertDir + "signedcrt.pem";
			
		PemX509Tools.setupCAandClientCert(testBaseDir, clientCertDir, clientCertLocation, clientCN, trustRootsDir);

		/* And verify from disk, read PEM certificates*/
		Debug.println("  Step 1 - Verify signed cert with trusted CA from fs");
		PemX509Tools.verifyCertificate(clientCertLocation, trustRootsDir);

		/* Get common name from certificate */
		Debug.println("  Step 2 - Get CN from verified cert");
		X509Info x509 = new PemX509Tools().getUserIdFromCertificate(PemX509Tools.readCertificateFromPEMFile(clientCertLocation));
		if(x509 != null){
			Debug.println("  CN = ["+ x509.getCN()+"]");
		}
		assertThat(x509.getCN(),is(clientCN));
	}


}
