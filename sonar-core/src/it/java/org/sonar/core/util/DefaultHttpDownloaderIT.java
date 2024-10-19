/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Ignore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.connect.SocketConnection;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.SonarException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.HttpDownloader.HttpException;

class DefaultHttpDownloaderIT {

  @TempDir
  private File temporaryFolder;

  private static SocketConnection socketConnection;
  private static String baseUrl;

  @BeforeAll
  static void startServer() throws IOException {
    socketConnection = new SocketConnection(new ContainerServer(new Container() {
      public void handle(Request req, Response resp) {
        try {
          if (req.getPath().getPath().contains("/redirect/")) {
            resp.setCode(303);
            resp.setValue("Location", "/redirected");
          } else if (req.getPath().getPath().contains("/redirected")) {
            resp.getPrintStream().append("redirected");
          } else if (req.getPath().getPath().contains("/error")) {
            writeErrorResponse(req, resp);
          } else {
            writeDefaultResponse(req, resp);
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
    }));
    SocketAddress address = socketConnection.connect(new InetSocketAddress("localhost", 0));

    baseUrl = String.format("http://%s:%d", ((InetSocketAddress) address).getAddress().getHostAddress(), ((InetSocketAddress) address).getPort());
  }

  private static PrintStream writeDefaultResponse(Request req, Response resp) throws IOException {
    return resp.getPrintStream().append("agent=" + req.getValues("User-Agent").get(0));
  }

  private static void writeErrorResponse(Request req, Response resp) throws IOException {
    resp.setCode(500);
    resp.getPrintStream().append("agent=" + req.getValues("User-Agent").get(0));
  }

  @AfterAll
  static void stopServer() throws IOException {
    if (null != socketConnection) {
      socketConnection.close();
    }
  }

  @Test
  // To disable the timeout in debug mode, run the test with -Djunit.jupiter.execution.timeout.mode=disabled_on_debug
  @Timeout(10)
  void openStream_network_errors() throws IOException, URISyntaxException {
    // host not accepting connections
    String url = "http://127.0.0.1:1";

    assertThatThrownBy(() -> {
      DefaultHttpDownloader downloader = new DefaultHttpDownloader(mock(Server.class), new MapSettings().asConfig());
      downloader.openStream(new URI(url));
    })
      .isInstanceOf(SonarException.class)
      .isEqualToComparingFieldByField(new BaseMatcher<Exception>() {
        @Override
        public boolean matches(Object ex) {
          return ex instanceof SonarException && ((SonarException) ex).getCause() instanceof SocketTimeoutException;
        }

        @Override
        public void describeTo(Description arg0) {
        }
      });
  }

  @Test
  void downloadBytes() throws URISyntaxException {
    byte[] bytes = new DefaultHttpDownloader(mock(Server.class), new MapSettings().asConfig()).readBytes(new URI(baseUrl));
    assertThat(bytes).hasSizeGreaterThan(10);
  }

  @Test
  void readString() throws URISyntaxException {
    String text = new DefaultHttpDownloader(mock(Server.class), new MapSettings().asConfig()).readString(new URI(baseUrl), StandardCharsets.UTF_8);
    assertThat(text.length()).isGreaterThan(10);
  }

  @Test
  void readStringWithDefaultTimeout() throws URISyntaxException {
    String text = new DefaultHttpDownloader(mock(Server.class), new MapSettings().asConfig()).readString(new URI(baseUrl + "/timeout/"), StandardCharsets.UTF_8);
    assertThat(text.length()).isGreaterThan(10);
  }

  @Test
  void downloadToFile() throws URISyntaxException, IOException {
    File toFile = new File(temporaryFolder, "downloadToFile.txt");

    new DefaultHttpDownloader(mock(Server.class), new MapSettings().asConfig()).download(new URI(baseUrl), toFile);
    assertThat(toFile).exists();
    assertThat(toFile.length()).isGreaterThan(10L);
  }

  @Test
  void shouldNotCreateFileIfFailToDownload() throws Exception {
    File toFile = new File(temporaryFolder, "downloadToFile.txt");

    try {
      new DefaultHttpDownloader(mock(Server.class), new MapSettings().asConfig()).download(new URI("http://localhost:1"), toFile);
    } catch (SonarException e) {
      assertThat(toFile).doesNotExist();
    }
  }

  @Test
  void userAgent_includes_version_and_SERVER_ID_when_server_is_provided() throws URISyntaxException, IOException {
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
  void userAgent_includes_only_version_when_there_is_no_SERVER_ID_and_server_is_provided() throws URISyntaxException, IOException {
    Server server = mock(Server.class);
    when(server.getVersion()).thenReturn("2.2");

    InputStream stream = new DefaultHttpDownloader(server, new MapSettings().asConfig()).openStream(new URI(baseUrl));
    Properties props = new Properties();
    props.load(stream);
    stream.close();

    assertThat(props.getProperty("agent")).isEqualTo("SonarQube 2.2 #");
  }

  @Test
  void followRedirect() throws URISyntaxException {
    String content = new DefaultHttpDownloader(mock(Server.class), new MapSettings().asConfig()).readString(new URI(baseUrl + "/redirect/"), StandardCharsets.UTF_8);
    assertThat(content).isEqualTo("redirected");
  }

  @Test
  void supported_schemes() {
    assertThat(new DefaultHttpDownloader(mock(Server.class), new MapSettings().asConfig()).getSupportedSchemes()).contains("http");
  }

  @Test
  void uri_description() throws URISyntaxException {
    String description = new DefaultHttpDownloader(mock(Server.class), new MapSettings().asConfig()).description(new URI("http://sonarsource.org"));
    assertThat(description).isEqualTo("http://sonarsource.org");
  }

  @Test
  void readBytes_whenServerReturnsError_shouldThrow() throws URISyntaxException {
    DefaultHttpDownloader downloader = new DefaultHttpDownloader(mock(Server.class), new MapSettings().asConfig());
    URI errorUri = new URI(baseUrl + "/error");
    assertThatThrownBy(() -> downloader.readBytes(errorUri)).isInstanceOf(HttpException.class).hasMessage("Fail to download [" + errorUri + "]. Response code: 500");
  }

}
