package nl.knmi.adaguc.authentication;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.util.UUID;

import javax.annotation.Resource;

import org.apache.http.impl.client.HttpClients;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import nl.knmi.adaguc.security.PemX509Tools;
import nl.knmi.adaguc.security.PemX509Tools.X509Info;
import nl.knmi.adaguc.security.token.TokenManager;
import nl.knmi.adaguc.tools.Debug;


@RunWith(SpringRunner.class)
@SpringBootTest
public class AuthenticatorImplTest {

	/** Entry point for Spring MVC testing support. */
	private MockMvc mockMvc;

	/** The Spring web application context. */
	@Resource
	private WebApplicationContext webApplicationContext;

	@Before
	public void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
				.build();
		HttpClients.createDefault();

	}


	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void TestGetTokenFromPath() throws Exception{
		TokenManager tk = new TokenManager();
		String token = tk.getTokenFromPath("/test/bla/a02b717b-02d6-4db9-bddf-a1d3774fee87/bla/bla.nc");
		Debug.println(token);
		assertThat(token,is("a02b717b-02d6-4db9-bddf-a1d3774fee87"));
		String tokenNope = tk.getTokenFromPath("/test/bla/np-a02b717b-02d6-4db9-bddf-a1d3774fee87.test/bla/bla.nc");
		Debug.println(tokenNope);
		assertThat(tokenNope,org.hamcrest.Matchers.isEmptyOrNullString());
	}
	
//	@Test
//	public void TestDAPDDS() throws Exception{
//	
//		TinyDapServer.handleOpenDapReqeuests("/home/c3smagic/Downloads/test-metric.nc","/","/",null,null);
//	}

	@Test
	public void TestThis() throws Exception{
		//Authenticator authenticator = new Authenticator();
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
		MvcResult result = mockMvc.perform(get("/user/getuserinfofromcert")
				.sessionAttr("javax.servlet.request.X509Certificate", PemX509Tools.readCertificateFromPEMFile(clientCertLocation))
				.contentType(MediaType.APPLICATION_JSON_UTF8).content("{}"))
				//                .andExpect(status().isMethodNotAllowed())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
				.andReturn();
		String responseBody = result.getResponse().getContentAsString();
		Debug.println(responseBody);
//		ObjectNode jsonResult = (ObjectNode) objectMapper.readTree(responseBody);
		//        assertThat(jsonResult.has("error"), is(true));
		//        assertThat(jsonResult.get("error").asText().length(), not(0));

	}

//
//
//	private static final char[] KEYPASS_VALUE = "password".toCharArray();
//	private static final char[] STOREPASS_VALUE = "changeit".toCharArray();	
//	private static final String SERVER_KEYSTORE = "/home/c3smagic/impactportal/c4i_keystore.jks";
//	private static final String CLIENT_KEYSTORE = "/home/c3smagic/impactportal/c4i_keystore.jks";
//	private static final String CLIENT_TRUSTSTORE = "/home/c3smagic/config/esg-truststore.ts";
//	protected String getBaseUrl(HttpServer server) {
//		return server.getInetAddress().getHostName() + ":" + server.getLocalPort();
//	}
//	
//
//	private static final String JAVA_KEYSTORE = "jks";
//
//	/*
//	 * KeyStores provide credentials, TrustStores verify credentials.
//
//	 * Server KeyStores stores the server's private keys, and certificates for corresponding public
//	 * keys. Used here for HTTPS connections over localhost.
//
//	 * Client TrustStores store servers' certificates.
//	 */
//	public static KeyStore getStore(final String storeFileName, final char[] password) throws
//	KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
//		final KeyStore store = KeyStore.getInstance(JAVA_KEYSTORE);
//		//		Debug.println("Opening store "+storeFileName);
//		//		URL url = getClass().getClassLoader().getResource(storeFileName);
//		InputStream inputStream = new FileInputStream(storeFileName);
//		try {
//			store.load(inputStream,password);
//			//			store.load(inputStream, password);
//		} finally {
//			inputStream.close();
//		}
//
//		return store;
//	}
//
//	/*
//	 * KeyManagers decide which authentication credentials (e.g. certs) should be sent to the remote
//	 * host for authentication during the SSL handshake.
//
//	 * Server KeyManagers use their private keys during the key exchange algorithm and send
//	 * certificates corresponding to their public keys to the clients. The certificate comes from
//	 * the KeyStore.
//	 */
//	protected static KeyManager[] getKeyManagers(KeyStore store, final char[] password) throws
//	NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
//		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
//				KeyManagerFactory.getDefaultAlgorithm());
//		keyManagerFactory.init(store, password);
//
//		return keyManagerFactory.getKeyManagers();
//	}
//
//	/*
//	 * TrustManagers determine if the remote connection should be trusted or not.
//
//	 * Clients will use certificates stored in their TrustStores to verify identities of servers.
//	 * Servers will use certificates stored in their TrustStores to verify identities of clients.
//	 */
//	protected static TrustManager[] getTrustManagers(KeyStore store) throws NoSuchAlgorithmException,
//	KeyStoreException {
//		TrustManagerFactory trustManagerFactory =
//				TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
//		trustManagerFactory.init(store);
//
//		return trustManagerFactory.getTrustManagers();
//	}
//
//
//
//	/*
//	Create an SSLContext for the server using the server's JKS. This instructs the server to
//	present its certificate when clients connect over HTTPS.
//	 */
//	public static SSLContext createServerSSLContext(final String storeFileName, final char[]
//			password) throws CertificateException, NoSuchAlgorithmException, KeyStoreException,
//	IOException, UnrecoverableKeyException, KeyManagementException {
//		KeyStore serverKeyStore = getStore(storeFileName, password);
//		KeyManager[] serverKeyManagers = getKeyManagers(serverKeyStore, password);
//		TrustManager[] serverTrustManagers = getTrustManagers(serverKeyStore);
//
//		SSLContext sslContext = SSLContexts.custom().useProtocol("TLS").build();
//		sslContext.init(serverKeyManagers, serverTrustManagers, new SecureRandom());
//
//		return sslContext;
//	}
//	public static HttpServer createLocalTestServer(SSLContext sslContext, boolean forceSSLAuth)
//			throws UnknownHostException {
//		final HttpServer server = ServerBootstrap.bootstrap()
//				.setLocalAddress(Inet4Address.getByName("localhost"))
//				.setSslContext(sslContext)
//				.setSslSetupHandler(socket -> socket.setNeedClientAuth(forceSSLAuth))
//				.registerHandler("*",
//						(request, response, context) -> response.setStatusCode(HttpStatus.SC_OK))
//				.create();
//
//		return server;
//	}
//
//	@Test
//	public void httpsRequest_With2WaySSLAndHasValidKeyStoreAndTrustStore_Returns200OK()
//			throws Exception {
//		SSLContext serverSSLContext =
//				createServerSSLContext(SERVER_KEYSTORE, KEYPASS_VALUE);
//
//		final HttpServer server = createLocalTestServer(serverSSLContext, true);
//		server.start();
//
//		String baseUrl = getBaseUrl(server);
//
//		KeyStore clientTrustStore = getStore(CLIENT_TRUSTSTORE, STOREPASS_VALUE);
//		KeyStore clientKeyStore = getStore(CLIENT_KEYSTORE, KEYPASS_VALUE);
//
//		SSLContext sslContext =
//				new SSLContextBuilder()
//				.loadTrustMaterial(clientTrustStore, new TrustSelfSignedStrategy())
//				.loadKeyMaterial(clientKeyStore, KEYPASS_VALUE)
//				.build();
//
//		httpclient = HttpClients.custom().setSSLContext(sslContext).build();
//
//		try {
//			CloseableHttpResponse httpResponse = httpclient.execute(
//					new HttpGet("https://compute-test.c3s-magic.eu:9000/user/getuserinfofromcert"));
//			String result = EntityUtils.toString(httpResponse.getEntity()); // DebugConsole.println("Content:\n"+EntityUtils.toString(entity);
//			Debug.println(result);
//			assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(200));
//		} finally {
//			server.stop();
//		}
//	}




}
