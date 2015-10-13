/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package server;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.util.NetworkUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class HttpsTest {

  public static final String HTTPS_PROTOCOLS = "https.protocols";

  Orchestrator orchestrator;

  int httpsPort = NetworkUtils.getNextAvailablePort();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  String initialHttpsProtocols = null;

  @Before
  public void setUp() throws Exception {
    // SSLv3 is not supported since SQ 4.5.2. Only TLS v1, v1.1 and v1.2 are
    // enabled by Tomcat.
    // The problem is that java 1.6 supports only TLSv1 but not v1.1 nor 1.2,
    // so version to be used must be explicitly set on JVM.
    initialHttpsProtocols = StringUtils.defaultString(System.getProperty(HTTPS_PROTOCOLS), "");
    System.setProperty(HTTPS_PROTOCOLS, "TLSv1");
  }

  @After
  public void tearDown() {
    if (orchestrator != null) {
      orchestrator.stop();
    }
    System.setProperty(HTTPS_PROTOCOLS, initialHttpsProtocols);
  }

  @Test
  public void fail_to_start_if_bad_keystore_credentials() throws Exception {
    try {
      URL jksKeystore = getClass().getResource("/server/HttpsTest/keystore.jks");
      orchestrator = Orchestrator.builderEnv()
        .setServerProperty("sonar.web.https.port", String.valueOf(httpsPort))
        .setServerProperty("sonar.web.https.keyAlias", "tests")
        .setServerProperty("sonar.web.https.keyPass", "__wrong__")
        .setServerProperty("sonar.web.https.keystoreFile", new File(jksKeystore.toURI()).getAbsolutePath())
        .setServerProperty("sonar.web.https.keystorePass", "__wrong__")
        .build();
      orchestrator.start();
      fail();
    } catch (Exception e) {
      File logFile = orchestrator.getServer().getLogs();
      assertThat(FileUtils.readFileToString(logFile)).contains("Password verification failed");
    }
  }

  @Test
  public void enable_https_port() throws Exception {
    // start server
    URL jksKeystore = getClass().getResource("/server/HttpsTest/keystore.jks");
    orchestrator = Orchestrator.builderEnv()
      .setServerProperty("sonar.web.https.port", String.valueOf(httpsPort))
      .setServerProperty("sonar.web.https.keyAlias", "tests")
      .setServerProperty("sonar.web.https.keyPass", "thetests")
      .setServerProperty("sonar.web.https.keystoreFile", new File(jksKeystore.toURI()).getAbsolutePath())
      .setServerProperty("sonar.web.https.keystorePass", "thepassword")
      .build();
    orchestrator.start();

    // check logs
    File logFile = orchestrator.getServer().getLogs();
    assertThat(FileUtils.readFileToString(logFile)).contains("HTTPS connector enabled on port " + httpsPort);

    // connect from clients
    connectTrusted();
    connectUntrusted();
  }

  private void connectTrusted() throws IOException {
    URL url = new URL("https://localhost:" + httpsPort + "/sonar");
    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
    try {
      connection.getInputStream();
      fail();
    } catch (SSLHandshakeException e) {
      // ok, the certificate is not trusted
    }
  }

  private void connectUntrusted() throws Exception {
    // Create a trust manager that does not validate certificate chains
    TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
      public X509Certificate[] getAcceptedIssuers() {
        return null;
      }

      public void checkClientTrusted(X509Certificate[] certs, String authType) {
      }

      public void checkServerTrusted(X509Certificate[] certs, String authType) {
      }
    }
    };

    // Install the all-trusting trust manager
    // SSLv3 is disabled since SQ 4.5.2 : https://jira.codehaus.org/browse/SONAR-5860
    SSLContext sc = SSLContext.getInstance("TLS");
    sc.init(null, trustAllCerts, new java.security.SecureRandom());

    SSLSocketFactory untrustedSocketFactory = sc.getSocketFactory();


    // Create all-trusting host name verifier
    HostnameVerifier allHostsValid = new HostnameVerifier() {
      public boolean verify(String hostname, SSLSession session) {
        return true;
      }
    };
    URL url = new URL("https://localhost:" + httpsPort + "/sonar/sessions/login");
    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
    connection.setRequestMethod("POST");
    connection.setAllowUserInteraction(true);
    connection.setSSLSocketFactory(untrustedSocketFactory);
    connection.setHostnameVerifier(allHostsValid);

    InputStream input = connection.getInputStream();
    checkCookieFlags(connection);
    try {
      String html = IOUtils.toString(input);
      assertThat(html).contains("<body");
    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  /**
   * SSF-13 HttpOnly flag
   * SSF-16 Secure flag
   */
  private void checkCookieFlags(HttpsURLConnection connection) {
    List<String> cookies = connection.getHeaderFields().get("Set-Cookie");
    boolean foundSessionCookie = false;
    for (String cookie : cookies) {
      if (StringUtils.containsIgnoreCase(cookie, "JSESSIONID")) {
        foundSessionCookie = true;
        assertThat(cookie).containsIgnoringCase("Secure").containsIgnoringCase("HttpOnly");
      }
    }
    if (!foundSessionCookie) {
      fail("Session cookie not found");
    }
  }
}
