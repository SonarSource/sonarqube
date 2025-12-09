/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.app;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.apache.catalina.connector.Connector;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.process.NetworkUtilsImpl;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmbeddedTomcatTest {

  private Path tempDir;
  
  @BeforeEach
  void setUp() throws IOException {
    tempDir = Files.createTempDirectory("temp-folder");
  }

  @Test
  void start_shouldStartTomcatAndAcceptConnections() throws Exception {
    InetAddress address = InetAddress.getLoopbackAddress();
    int httpPort = NetworkUtilsImpl.INSTANCE.getNextLoopbackAvailablePort();
    Props props = getProps(address, httpPort);

    EmbeddedTomcat tomcat = new EmbeddedTomcat(props, new TomcatHttpConnectorFactory());
    assertThat(tomcat.getStatus()).isEqualTo(EmbeddedTomcat.Status.DOWN);
    tomcat.start();
    assertThat(tomcat.getStatus()).isEqualTo(EmbeddedTomcat.Status.UP);

    URL url = new URI("http://" + address.getHostAddress() + ":" + httpPort).toURL();
    assertThatCode(() -> url.openConnection().connect())
      .doesNotThrowAnyException();
  }

  @Test
  void start_whenWrongScheme_shouldThrow() throws IOException {
    InetAddress address = InetAddress.getLoopbackAddress();
    int httpPort = NetworkUtilsImpl.INSTANCE.getNextLoopbackAvailablePort();
    Props props = getProps(address, httpPort);

    TomcatHttpConnectorFactory tomcatHttpConnectorFactory = mock();
    when(tomcatHttpConnectorFactory.createConnector(props)).thenReturn(getAJPConnector(props));
    EmbeddedTomcat tomcat = new EmbeddedTomcat(props, tomcatHttpConnectorFactory);

    assertThatThrownBy(tomcat::start)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(String.format("Unsupported connector: Connector[\"ajp-nio-127.0.0.1-%s\"]", httpPort));
  }

  private Connector getAJPConnector(Props props) {
    Connector connector = new Connector("AJP/1.3");
    connector.setScheme("ajp");
    connector.setPort(props.valueAsInt("sonar.web.port"));
    connector.setProperty("secretRequired", "false");
    return connector;
  }

  @Test
  void terminate_shouldTerminateTomcatAndStopAcceptingConnections() throws Exception {
    InetAddress address = InetAddress.getLoopbackAddress();
    int httpPort = NetworkUtilsImpl.INSTANCE.getNextLoopbackAvailablePort();
    Props props = getProps(address, httpPort);

    EmbeddedTomcat tomcat = new EmbeddedTomcat(props, new TomcatHttpConnectorFactory());
    tomcat.start();
    URL url = new URI("http://" + address.getHostAddress() + ":" + httpPort).toURL();

    tomcat.terminate();

    assertThatThrownBy(() -> url.openConnection().connect())
      .isInstanceOf(ConnectException.class)
      .hasMessageContaining("Connection refused");

  }

  private Props getProps(InetAddress address, int httpPort) throws IOException {
    Props props = new Props(new Properties());

    File home = new File(tempDir.toFile(), "homeDir");
    File data = new File(tempDir.toFile(), "dataDir");
    File webDir = new File(home, "web");
    FileUtils.writeByteArrayToFile(new File(home, "web/WEB-INF/web.xml"), "<web-app/>".getBytes(StandardCharsets.UTF_8));
    props.set("sonar.path.home", home.getAbsolutePath());
    props.set("sonar.path.data", data.getAbsolutePath());
    props.set("sonar.path.web", webDir.getAbsolutePath());
    props.set("sonar.path.logs", new File(tempDir.toFile(), "logsDir").getAbsolutePath());

    // start server on a random port
    props.set("sonar.web.host", address.getHostAddress());
    props.set("sonar.web.port", String.valueOf(httpPort));
    return props;
  }

}
