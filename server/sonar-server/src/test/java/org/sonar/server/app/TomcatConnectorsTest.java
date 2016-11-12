/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TomcatConnectorsTest {

  Tomcat tomcat = mock(Tomcat.class, Mockito.RETURNS_DEEP_STUBS);

  // ---- connectors

  @Test
  public void configure_thread_pool() {
    Properties p = new Properties();
    p.setProperty("sonar.web.http.minThreads", "2");
    p.setProperty("sonar.web.http.maxThreads", "30");
    p.setProperty("sonar.web.http.acceptCount", "20");
    Props props = new Props(p);

    TomcatConnectors.configure(tomcat, props);

    verify(tomcat).setConnector(argThat(new PropertiesMatcher(
      ImmutableMap.<String, Object>of("minSpareThreads", 2, "maxThreads", 30, "acceptCount", 20))));
  }

  @Test
  public void configure_default_thread_pool() {
    Props props = new Props(new Properties());

    TomcatConnectors.configure(tomcat, props);

    verify(tomcat).setConnector(argThat(new PropertiesMatcher(
      ImmutableMap.<String, Object>of("minSpareThreads", 5, "maxThreads", 50, "acceptCount", 25))));
  }

  @Test
  public void different_thread_pools_for_connectors() {
    Properties p = new Properties();
    p.setProperty("sonar.web.port", "9000");
    p.setProperty("sonar.web.http.minThreads", "2");
    Props props = new Props(p);

    TomcatConnectors.configure(tomcat, props);

    verify(tomcat.getService()).addConnector(argThat(new ArgumentMatcher<Connector>() {
      @Override
      public boolean matches(Object o) {
        Connector c = (Connector) o;
        return c.getPort() == 9000 && c.getProperty("minSpareThreads").equals(2);
      }
    }));
  }

  @Test
  public void http_connector_is_enabled() {
    Properties p = new Properties();
    p.setProperty("sonar.web.port", "9000");
    Props props = new Props(p);

    TomcatConnectors.configure(tomcat, props);

    verify(tomcat.getService()).addConnector(argThat(new ArgumentMatcher<Connector>() {
      @Override
      public boolean matches(Object o) {
        Connector c = (Connector) o;
        return c.getScheme().equals("http") && c.getPort() == 9000 && c.getProtocol().equals(TomcatConnectors.HTTP_PROTOCOL);
      }
    }));
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

    verify(tomcat.getService()).addConnector(argThat(new ArgumentMatcher<Connector>() {
      @Override
      public boolean matches(Object o) {
        Connector c = (Connector) o;
        return c.getScheme().equals("http") && c.getPort() == 9000 && ((InetAddress) c.getProperty("address")).getHostAddress().equals("0.0.0.0");
      }
    }));
  }

  @Test
  public void bind_to_specific_address() {
    Properties p = new Properties();
    p.setProperty("sonar.web.port", "9000");
    p.setProperty("sonar.web.host", "1.2.3.4");

    TomcatConnectors.configure(tomcat, new Props(p));

    verify(tomcat.getService()).addConnector(argThat(new ArgumentMatcher<Connector>() {
      @Override
      public boolean matches(Object o) {
        Connector c = (Connector) o;
        return c.getScheme().equals("http") && c.getPort() == 9000 && ((InetAddress) c.getProperty("address")).getHostAddress().equals("1.2.3.4");
      }
    }));
  }

  @Test
  public void test_max_http_header_size_for_http_connection() {
    Properties properties = new Properties();

    Props props = new Props(properties);
    TomcatConnectors.configure(tomcat, props);
    verifyConnectorProperty(tomcat, "http", "maxHttpHeaderSize", TomcatConnectors.MAX_HTTP_HEADER_SIZE_BYTES);
  }

  @Test
  public void test_max_post_size_for_http_connection() throws Exception {
    Properties properties = new Properties();

    Props props = new Props(properties);
    TomcatConnectors.configure(tomcat, props);
    verify(tomcat.getService()).addConnector(argThat(new ArgumentMatcher<Connector>() {
      @Override
      public boolean matches(Object o) {
        Connector c = (Connector) o;
        return c.getMaxPostSize() == -1;
      }
    }));
  }

  private static void verifyConnectorProperty(Tomcat tomcat, final String connectorScheme,
                                              final String property, final Object propertyValue) {
    verify(tomcat.getService()).addConnector(argThat(new ArgumentMatcher<Connector>() {
      @Override
      public boolean matches(Object o) {
        Connector c = (Connector) o;
        return c.getScheme().equals(connectorScheme) && c.getProperty(property).equals(propertyValue);
      }
    }));
  }

  private static class PropertiesMatcher extends ArgumentMatcher<Connector> {
    private final Map<String, Object> expected;

    PropertiesMatcher(Map<String, Object> expected) {
      this.expected = expected;
    }

    public boolean matches(Object o) {
      Connector c = (Connector) o;
      for (Map.Entry<String, Object> entry : expected.entrySet()) {
        if (!entry.getValue().equals(c.getProperty(entry.getKey()))) {
          return false;
        }
      }
      return true;
    }
  }
}
