package nl.knmi.adaguc.tools;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.operator.OperatorCreationException;
import org.ietf.jgss.GSSException;

import nl.knmi.adaguc.config.ConfigurationItemNotFoundException;
import nl.knmi.adaguc.security.CertificateVerificationException;
import nl.knmi.adaguc.security.PemX509Tools;
import nl.knmi.adaguc.security.SecurityConfigurator;

public class Main {

	
	public static void main(String[] args) throws CertificateException, IOException, CertificateVerificationException, NoSuchAlgorithmException, OperatorCreationException, InvalidKeyException, ConfigurationItemNotFoundException {

	
//		AuthenticatorImpl authenticator = new AuthenticatorImpl();
//
//		String clientCN = "ClientCert[" + UUID.randomUUID().toString()+"]";
//		String testBaseDir = "/tmp/adaguc-services-test/";
//		String trustRootsDir = testBaseDir + "trustedcertificates/";
//		String clientCertDir = testBaseDir + "signedcerts/";
//		String clientCertLocation = clientCertDir + "signedcrt.pem";
//			
//		PemX509Tools.setupCAandClientCert(testBaseDir, clientCertDir, clientCertLocation, clientCN, trustRootsDir);
//
//		/* And verify from disk, read PEM certificates*/
//		Debug.println("  Step 1 - Verify signed cert with trusted CA from fs");
//		PemX509Tools.verifyCertificate(clientCertLocation, trustRootsDir);
//
//		/* Get common name from certificate */
//		Debug.println("  Step 2 - Get CN from verified cert");
//		X509Info x509 = new PemX509Tools().getUserIdFromCertificate(PemX509Tools.readCertificateFromPEM(clientCertLocation));
//		if(x509 != null){
//			Debug.println("  CN = ["+ x509.getCN()+"]");
//		}
		
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		try {
			char [] CLIENT_TRUSTSTORE_PASSWORD = SecurityConfigurator.getTrustStorePassword().toCharArray();
			String CLIENT_TRUSTSTORE = SecurityConfigurator.getTrustStore();
			String certLoc = "/home/c3smagic/impactspace/esg-dn1.nsc.liu.se.esgf-idp.openid.maartenplieger/certs/creds.pem";
			String url = "https://compute-test.c3s-magic.eu:9000/wps?service=WPS&request=getcapabilities";
			CloseableHttpClient httpClient = (new PemX509Tools()).getHTTPClientForPEMBasedClientAuth(CLIENT_TRUSTSTORE, CLIENT_TRUSTSTORE_PASSWORD, certLoc);
			CloseableHttpResponse httpResponse = httpClient.execute(new HttpGet(url));
			String result = EntityUtils.toString(httpResponse.getEntity());
			Debug.println(result);

		}  catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SignatureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GSSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}


}
