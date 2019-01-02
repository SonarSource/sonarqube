/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;
import org.simpleframework.transport.connect.SocketConnection;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.SonarException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.internal.matchers.ThrowableCauseMatcher.hasCause;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultHttpDownloaderTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TestRule safeguardTimeout = new DisableOnDebug(Timeout.seconds(60));

  private static SocketConnection socketConnection;
  private static String baseUrl;

  @BeforeClass
  public static void startServer() throws IOException {
    socketConnection = new SocketConnection(new Container() {
      public void handle(Request req, Response resp) {
        try {
          if (req.getPath().getPath().contains("/redirect/")) {
            resp.setCode(303);
            resp.add("Location", "/");
          } else {
            if (req.getPath().getPath().contains("/timeout/")) {
              try {
                Thread.sleep(500);
              } catch (InterruptedException e) {
                throw new IllegalStateException(e);
              }
            }
            if (req.getPath().getPath().contains("/gzip/")) {
              if (!"gzip".equals(req.getValue("Accept-Encoding"))) {
                throw new IllegalStateException("Should accept gzip");
              }
              resp.set("Content-Encoding", "gzip");
              GZIPOutputStream gzipOutputStream = new GZIPOutputStream(resp.getOutputStream());
              gzipOutputStream.write("GZIP response".getBytes());
              gzipOutputStream.close();
            } else {
              resp.getPrintStream().append("agent=" + req.getValues("User-Agent").get(0));
            }
          }
        } catch (IOException e) {
          throw new IllegalStateException(e);
        } finally {
          try {
            resp.close();
          } catch (IOException ignored) {
          }
        }
      }
    });
    SocketAddress address = socketConnection.connect(new InetSocketAddress("localhost", 0));

    baseUrl = String.format("http://%s:%d", ((InetSocketAddress) address).getAddress().getHostAddress(), ((InetSocketAddress) address).getPort());
  }

  @AfterClass
  public static void stopServer() throws IOException {
    if (null != socketConnection) {
      socketConnection.close();
    }
  }

  @Test(timeout = 10000)
  public void openStream_network_errors() throws IOException, URISyntaxException {
    // non routable address
    String url = "http://10.255.255.1";

    thrown.expect(SonarException.class);
    thrown.expect(hasCause(new BaseMatcher<Exception>() {
      @Override
      public boolean matches(Object ex) {
        return
        // Java 8
        ex instanceof NoRouteToHostException || ex instanceof SocketException
        // Java 7 or before
          || ex instanceof SocketTimeoutException;
      }

      @Override
      public void describeTo(Description arg0) {
      }
    }));
    DefaultHttpDownloader downloader = new DefaultHttpDownloader(new MapSettings().asConfig(), 10, 50000);
    downloader.openStream(new URI(url));
  }

  @Test
  public void downloadBytes() throws URISyntaxException {
    byte[] bytes = new DefaultHttpDownloader(new MapSettings().asConfig()).readBytes(new URI(baseUrl));
    assertThat(bytes.length).isGreaterThan(10);
  }

  @Test
  public void readString() throws URISyntaxException {
    String text = new DefaultHttpDownloader(new MapSettings().asConfig()).readString(new URI(baseUrl), StandardCharsets.UTF_8);
    assertThat(text.length()).isGreaterThan(10);
  }

  @Test
  public void readGzipString() throws URISyntaxException {
    String text = new DefaultHttpDownloader(new MapSettings().asConfig()).readString(new URI(baseUrl + "/gzip/"), StandardCharsets.UTF_8);
    assertThat(text).isEqualTo("GZIP response");
  }

  @Test
  public void readStringWithDefaultTimeout() throws URISyntaxException {
    String text = new DefaultHttpDownloader(new MapSettings().asConfig()).readString(new URI(baseUrl + "/timeout/"), StandardCharsets.UTF_8);
    assertThat(text.length()).isGreaterThan(10);
  }

  @Test
  public void readStringWithTimeout() throws URISyntaxException {
    thrown.expect(new BaseMatcher<Exception>() {
      @Override
      public boolean matches(Object ex) {
        return ex instanceof SonarException && ((SonarException) ex).getCause() instanceof SocketTimeoutException;
      }

      @Override
      public void describeTo(Description arg0) {
      }
    });
    new DefaultHttpDownloader(new MapSettings().asConfig(), 50).readString(new URI(baseUrl + "/timeout/"), StandardCharsets.UTF_8);
  }

  @Test
  public void downloadToFile() throws URISyntaxException, IOException {
    File toDir = temporaryFolder.newFolder();
    File toFile = new File(toDir, "downloadToFile.txt");

    new DefaultHttpDownloader(new MapSettings().asConfig()).download(new URI(baseUrl), toFile);
    assertThat(toFile).exists();
    assertThat(toFile.length()).isGreaterThan(10l);
  }

  @Test
  public void shouldNotCreateFileIfFailToDownload() throws Exception {
    File toDir = temporaryFolder.newFolder();
    File toFile = new File(toDir, "downloadToFile.txt");

    try {
      int port = new InetSocketAddress("localhost", 0).getPort();
      new DefaultHttpDownloader(new MapSettings().asConfig()).download(new URI("http://localhost:" + port), toFile);
    } catch (SonarException e) {
      assertThat(toFile).doesNotExist();
    }
  }

  @Test
  public void userAgent_includes_version_and_SERVER_ID_when_server_is_provided() throws URISyntaxException, IOException {
    Server server = mock(Server.class);
    when(server.getVersion()).thenReturn("2.2");
    MapSettings settings = new MapSettings();
    settings.setProperty(CoreProperties.SERVER_ID, "blablabla");

    InputStream stream = new DefaultHttpDownloader(server, settings.asConfig()).openStream(new URI(baseUrl));
    Properties props = new Properties();
    props.load(stream);
    stream.close();

    assertThat(props.getProperty("agent")).isEqualTo("SonarQube 2.2 # blablabla");
  }

  @Test
  public void userAgent_includes_only_version_when_there_is_no_SERVER_ID_and_server_is_provided() throws URISyntaxException, IOException {
    Server server = mock(Server.class);
    when(server.getVersion()).thenReturn("2.2");

    InputStream stream = new DefaultHttpDownloader(server, new MapSettings().asConfig()).openStream(new URI(baseUrl));
    Properties props = new Properties();
    props.load(stream);
    stream.close();

    assertThat(props.getProperty("agent")).isEqualTo("SonarQube 2.2 #");
  }

  @Test
  public void userAgent_is_static_value_when_server_is_not_provided() throws URISyntaxException, IOException {
    InputStream stream = new DefaultHttpDownloader(new MapSettings().asConfig()).openStream(new URI(baseUrl));
    Properties props = new Properties();
    props.load(stream);
    stream.close();

    assertThat(props.getProperty("agent")).isEqualTo("SonarQube");
  }

  @Test
  public void followRedirect() throws URISyntaxException {
    String content = new DefaultHttpDownloader(new MapSettings().asConfig()).readString(new URI(baseUrl + "/redirect/"), StandardCharsets.UTF_8);
    assertThat(content).contains("agent");
  }

  @Test
  public void shouldGetDirectProxySynthesis() throws URISyntaxException {
    ProxySelector proxySelector = mock(ProxySelector.class);
    when(proxySelector.select(any(URI.class))).thenReturn(Arrays.asList(Proxy.NO_PROXY));
    assertThat(DefaultHttpDownloader.BaseHttpDownloader.getProxySynthesis(new URI("http://an_url"), proxySelector)).isEqualTo("no proxy");
  }

  @Test
  public void shouldGetProxySynthesis() throws URISyntaxException {
    ProxySelector proxySelector = mock(ProxySelector.class);
    when(proxySelector.select(any(URI.class))).thenReturn(Arrays.<Proxy>asList(new FakeProxy()));
    assertThat(DefaultHttpDownloader.BaseHttpDownloader.getProxySynthesis(new URI("http://an_url"), proxySelector)).isEqualTo("HTTP proxy: /123.45.67.89:4040");
  }

  @Test
  public void supported_schemes() {
    assertThat(new DefaultHttpDownloader(new MapSettings().asConfig()).getSupportedSchemes()).contains("http");
  }

  @Test
  public void uri_description() throws URISyntaxException {
    String description = new DefaultHttpDownloader(new MapSettings().asConfig()).description(new URI("http://sonarsource.org"));
    assertThat(description).matches("http://sonarsource.org \\(.*\\)");
  }

  @Test
  public void configure_http_proxy_credentials() {
    DefaultHttpDownloader.AuthenticatorFacade system = mock(DefaultHttpDownloader.AuthenticatorFacade.class);
    MapSettings settings = new MapSettings();
    settings.setProperty("https.proxyHost", "1.2.3.4");
    settings.setProperty("http.proxyUser", "the_login");
    settings.setProperty("http.proxyPassword", "the_passwd");

    new DefaultHttpDownloader.BaseHttpDownloader(system, settings.asConfig(), null);

    verify(system).setDefaultAuthenticator(argThat(authenticator -> {
      DefaultHttpDownloader.ProxyAuthenticator a = (DefaultHttpDownloader.ProxyAuthenticator) authenticator;
      PasswordAuthentication authentication = a.getPasswordAuthentication();
      return authentication.getUserName().equals("the_login") &&
        new String(authentication.getPassword()).equals("the_passwd");
    }));
  }

  private static class FakeProxy extends Proxy {
    FakeProxy() {
      super(Type.HTTP, new InetSocketAddress("123.45.67.89", 4040));
    }
  }
}
