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

import java.net.Inet4Address;
import java.util.Properties;
import org.apache.catalina.connector.Connector;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.process.Props;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class TomcatHttpConnectorFactoryTest {
  @Rule
  public LogTester logTester = new LogTester();

  private static TomcatHttpConnectorFactory tomcatHttpConnectorFactory = new TomcatHttpConnectorFactory();

  @Test
  public void createConnector_shouldUseHardcodedPropertiesWhereNeeded() {
    Props props = getEmptyProps();
    Connector connector = tomcatHttpConnectorFactory.createConnector(props);

    // General properties
    assertThat(connector.getURIEncoding()).isEqualTo("UTF-8");
    assertThat(connector.getProperty("socket.soReuseAddress")).isEqualTo("true");
    assertThat(connector.getProperty("relaxedQueryChars")).isEqualTo("\"<>[\\]^`{|}");
    assertThat(connector.getProperty("maxHttpHeaderSize")).isEqualTo(49152);
    assertThat(connector.getMaxPostSize()).isEqualTo(-1);
    // Compression properties
    assertThat(connector.getProperty("compression")).isEqualTo("on");
    assertThat(connector.getProperty("compressionMinSize")).isEqualTo(1024);
    assertThat(connector.getProperty("compressibleMimeType")).isEqualTo("text/html,text/xml,text/plain,text/css,application/json,application/javascript,text/javascript");
  }

  @Test
  public void createConnector_whenPropertiesNotSet_shouldUseDefault() {
    Props props = getEmptyProps();
    Connector connector = tomcatHttpConnectorFactory.createConnector(props);

    // General properties
    assertAddress(connector.getProperty("address"), "0.0.0.0");
    // Port
    assertThat(connector.getPort()).isEqualTo(9000);
    // Pool properties
    assertThat(connector.getProperty("minSpareThreads")).isEqualTo(5);
    assertThat(connector.getProperty("maxThreads")).isEqualTo(50);
    assertThat(connector.getProperty("acceptCount")).isEqualTo(25);
    assertThat(connector.getProperty("keepAliveTimeout")).isEqualTo(60000);
  }

  @Test
  public void createConnector_whenPropertiesSet_shouldUseThem() {
    Props props = getMeaningfulProps();
    Connector connector = tomcatHttpConnectorFactory.createConnector(props);

    // General properties
    assertAddress(connector.getProperty("address"), "12.12.12.12");
    // Port
    assertThat(connector.getPort()).isEqualTo(1234);
    Assertions.assertThat(logTester.logs(Level.INFO)).contains("Starting Tomcat on port 1234");
    // Pool properties
    assertThat(connector.getProperty("minSpareThreads")).isEqualTo(12);
    assertThat(connector.getProperty("maxThreads")).isEqualTo(42);
    assertThat(connector.getProperty("acceptCount")).isEqualTo(12);
    assertThat(connector.getProperty("keepAliveTimeout")).isEqualTo(1000);
  }

  @Test
  public void createConnector_whenNotValidPort_shouldThrow() {
    Props props = getEmptyProps();
    props.set("sonar.web.port", "-1");
    assertThatThrownBy(() -> tomcatHttpConnectorFactory.createConnector(props))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("HTTP port -1 is invalid");
  }

  private void assertAddress(Object address, String ip) {
    assertThat(address).isInstanceOf(Inet4Address.class);
    Inet4Address inet4Address = (Inet4Address) address;
    assertThat(inet4Address.getHostAddress()).isEqualTo(ip);
  }

  private Props getEmptyProps() {
    return new Props(new Properties());
  }

  private Props getMeaningfulProps() {
    Properties properties = new Properties();
    properties.setProperty("sonar.web.host", "12.12.12.12");
    properties.setProperty("sonar.web.port", "1234");
    properties.setProperty("sonar.web.http.minThreads", "12");
    properties.setProperty("sonar.web.http.maxThreads", "42");
    properties.setProperty("sonar.web.http.acceptCount", "12");
    properties.setProperty("sonar.web.http.keepAliveTimeout", "1000");
    return new Props(properties);
  }

}