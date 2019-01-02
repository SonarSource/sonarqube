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
package org.sonar.server.app;

import com.google.common.collect.ImmutableMap;
import java.net.InetAddress;
import java.util.Map;
import java.util.Properties;
import org.apache.catalina.startup.Tomcat;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TomcatConnectorsTest {

  private static final int DEFAULT_PORT = 9000;
  private Tomcat tomcat = mock(Tomcat.class, Mockito.RETURNS_DEEP_STUBS);

  @Test
  public void configure_thread_pool() {
    Properties p = new Properties();
    p.setProperty("sonar.web.http.minThreads", "2");
    p.setProperty("sonar.web.http.maxThreads", "30");
    p.setProperty("sonar.web.http.acceptCount", "20");
    Props props = new Props(p);

    TomcatConnectors.configure(tomcat, props);

    verifyHttpConnector(DEFAULT_PORT, ImmutableMap.of("minSpareThreads", 2, "maxThreads", 30, "acceptCount", 20));
  }

  @Test
  public void configure_defaults() {
    Props props = new Props(new Properties());

    TomcatConnectors.configure(tomcat, props);

    verifyHttpConnector(DEFAULT_PORT, ImmutableMap.of("minSpareThreads", 5, "maxThreads", 50, "acceptCount", 25));
  }

  @Test
  public void different_thread_pools_for_connectors() {
    Properties p = new Properties();
    p.setProperty("sonar.web.http.minThreads", "2");
    Props props = new Props(p);

    TomcatConnectors.configure(tomcat, props);

    verifyHttpConnector(DEFAULT_PORT, ImmutableMap.of("minSpareThreads", 2));
  }

  @Test
  public void fail_with_ISE_if_http_port_is_invalid() {
    Properties p = new Properties();
    p.setProperty("sonar.web.port", "-1");

    try {
      TomcatConnectors.configure(tomcat, new Props(p));
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("HTTP port '-1' is invalid");
    }
  }

  @Test
  public void bind_to_all_addresses_by_default() {
    Properties p = new Properties();
    p.setProperty("sonar.web.port", "9000");

    TomcatConnectors.configure(tomcat, new Props(p));

    verify(tomcat.getService()).addConnector(argThat(c -> c.getScheme().equals("http") && c.getPort() == 9000 && ((InetAddress) c.getProperty("address")).getHostAddress().equals("0.0.0.0")));
  }

  @Test
  public void bind_to_specific_address() {
    Properties p = new Properties();
    p.setProperty("sonar.web.port", "9000");
    p.setProperty("sonar.web.host", "1.2.3.4");

    TomcatConnectors.configure(tomcat, new Props(p));

    verify(tomcat.getService())
      .addConnector(argThat(c -> c.getScheme().equals("http") && c.getPort() == 9000 && ((InetAddress) c.getProperty("address")).getHostAddress().equals("1.2.3.4")));
  }

  @Test
  public void test_max_http_header_size_for_http_connection() {
    TomcatConnectors.configure(tomcat, new Props(new Properties()));

    verifyHttpConnector(DEFAULT_PORT, ImmutableMap.of("maxHttpHeaderSize", TomcatConnectors.MAX_HTTP_HEADER_SIZE_BYTES));
  }

  @Test
  public void test_max_post_size_for_http_connection() {
    Properties properties = new Properties();

    Props props = new Props(properties);
    TomcatConnectors.configure(tomcat, props);
    verify(tomcat.getService()).addConnector(argThat(c -> c.getMaxPostSize() == -1));
  }

  private void verifyHttpConnector(int expectedPort, Map<String, Object> expectedProps) {
    verify(tomcat.getService()).addConnector(argThat(c -> {
      if (!c.getScheme().equals("http")) {
        return false;
      }
      if (!c.getProtocol().equals(TomcatConnectors.HTTP_PROTOCOL)) {
        return false;
      }
      if (c.getPort() != expectedPort) {
        return false;
      }
      for (Map.Entry<String, Object> expectedProp : expectedProps.entrySet()) {
        if (!expectedProp.getValue().equals(c.getProperty(expectedProp.getKey()))) {
          return false;
        }
      }
      return true;
    }));
  }
}
