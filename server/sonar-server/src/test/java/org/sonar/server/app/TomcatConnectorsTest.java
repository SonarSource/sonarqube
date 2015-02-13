
/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.sonar.process.Props;

import java.net.InetAddress;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TomcatConnectorsTest {

  Tomcat tomcat = mock(Tomcat.class, Mockito.RETURNS_DEEP_STUBS);

  // ---- connectors

  @Test
  public void configure_thread_pool() throws Exception {
    Properties p = new Properties();
    p.setProperty("sonar.web.http.minThreads", "2");
    p.setProperty("sonar.web.http.maxThreads", "30");
    p.setProperty("sonar.web.http.acceptCount", "20");
    Props props = new Props(p);

    TomcatConnectors.configure(tomcat, props);

    verify(tomcat).setConnector(argThat(new PropertiesMatcher(
      ImmutableMap.<String, Object>of("minSpareThreads", 2, "maxThreads", 30, "acceptCount", 20)
      )));
  }

  @Test
  public void configure_default_thread_pool() throws Exception {
    Props props = new Props(new Properties());

    TomcatConnectors.configure(tomcat, props);

    verify(tomcat).setConnector(argThat(new PropertiesMatcher(
      ImmutableMap.<String, Object>of("minSpareThreads", 5, "maxThreads", 50, "acceptCount", 25)
      )));
  }

  @Test
  public void different_thread_pools_for_connectors() throws Exception {
    Properties p = new Properties();
    p.setProperty("sonar.web.port", "9000");
    p.setProperty("sonar.web.http.minThreads", "2");
    p.setProperty("sonar.web.https.port", "9443");
    p.setProperty("sonar.web.https.minThreads", "5");
    Props props = new Props(p);

    TomcatConnectors.configure(tomcat, props);

    verify(tomcat.getService()).addConnector(argThat(new ArgumentMatcher<Connector>() {
      @Override
      public boolean matches(Object o) {
        Connector c = (Connector) o;
        return c.getPort() == 9000 && c.getProperty("minSpareThreads").equals(2);
      }
    }));
    verify(tomcat.getService()).addConnector(argThat(new ArgumentMatcher<Connector>() {
      @Override
      public boolean matches(Object o) {
        Connector c = (Connector) o;
        return c.getPort() == 9443 && c.getProperty("minSpareThreads").equals(5);
      }
    }));
  }

  @Test
  public void fail_if_http_connectors_are_disabled() {
    Properties p = new Properties();
    p.setProperty("sonar.web.port", "-1");
    p.setProperty("sonar.web.https.port", "-1");
    Props props = new Props(p);

    try {
      TomcatConnectors.configure(tomcat, props);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("HTTP connectors are disabled");
    }
  }

  @Test
  public void only_https_is_enabled() {
    Properties p = new Properties();
    p.setProperty("sonar.web.port", "-1");
    p.setProperty("sonar.web.https.port", "9443");
    Props props = new Props(p);

    TomcatConnectors.configure(tomcat, props);

    verify(tomcat).setConnector(argThat(new ArgumentMatcher<Connector>() {
      @Override
      public boolean matches(Object o) {
        Connector c = (Connector) o;
        return c.getScheme().equals("https") && c.getPort() == 9443
          && c.getProperty("clientAuth").equals("false");
      }
    }));
  }

  @Test
  public void all_connectors_are_enabled() {
    Properties p = new Properties();
    p.setProperty("sonar.web.port", "9000");
    p.setProperty("sonar.ajp.port", "9009");
    p.setProperty("sonar.web.https.port", "9443");
    Props props = new Props(p);

    TomcatConnectors.configure(tomcat, props);

    verify(tomcat.getService()).addConnector(argThat(new ArgumentMatcher<Connector>() {
      @Override
      public boolean matches(Object o) {
        Connector c = (Connector) o;
        return c.getScheme().equals("http") && c.getPort() == 9000 && c.getProtocol().equals(TomcatConnectors.HTTP_PROTOCOL);
      }
    }));
    verify(tomcat.getService()).addConnector(argThat(new ArgumentMatcher<Connector>() {
      @Override
      public boolean matches(Object o) {
        Connector c = (Connector) o;
        return c.getScheme().equals("http") && c.getPort() == 9009 && c.getProtocol().equals(TomcatConnectors.AJP_PROTOCOL);
      }
    }));
    verify(tomcat.getService()).addConnector(argThat(new ArgumentMatcher<Connector>() {
      @Override
      public boolean matches(Object o) {
        Connector c = (Connector) o;
        return c.getScheme().equals("https") && c.getPort() == 9443 && c.getProtocol().equals(TomcatConnectors.HTTP_PROTOCOL);
      }
    }));
  }

  @Test
  public void http_and_ajp_and_https_ports_should_be_different() throws Exception {
    Properties p = new Properties();
    p.setProperty("sonar.web.port", "9000");
    p.setProperty("sonar.ajp.port", "9000");
    p.setProperty("sonar.web.https.port", "9000");

    try {
      TomcatConnectors.configure(tomcat, new Props(p));
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("HTTP, AJP and HTTPS must not use the same port 9000");
    }
  }

  @Test
  public void bind_to_all_addresses_by_default() throws Exception {
    Properties p = new Properties();
    p.setProperty("sonar.web.port", "9000");
    p.setProperty("sonar.ajp.port", "9009");
    p.setProperty("sonar.web.https.port", "9443");

    TomcatConnectors.configure(tomcat, new Props(p));

    verify(tomcat.getService()).addConnector(argThat(new ArgumentMatcher<Connector>() {
      @Override
      public boolean matches(Object o) {
        Connector c = (Connector) o;
        return c.getScheme().equals("http") && c.getPort() == 9000 && ((InetAddress) c.getProperty("address")).getHostAddress().equals("0.0.0.0");
      }
    }));
    verify(tomcat.getService()).addConnector(argThat(new ArgumentMatcher<Connector>() {
      @Override
      public boolean matches(Object o) {
        Connector c = (Connector) o;
        return c.getScheme().equals("http") && c.getPort() == 9009 && ((InetAddress) c.getProperty("address")).getHostAddress().equals("0.0.0.0");
      }
    }));
    verify(tomcat.getService()).addConnector(argThat(new ArgumentMatcher<Connector>() {
      @Override
      public boolean matches(Object o) {
        Connector c = (Connector) o;
        return c.getScheme().equals("https") && c.getPort() == 9443 && ((InetAddress) c.getProperty("address")).getHostAddress().equals("0.0.0.0");
      }
    }));
  }

  @Test
  public void bind_to_specific_address() throws Exception {
    Properties p = new Properties();
    p.setProperty("sonar.web.port", "9000");
    p.setProperty("sonar.web.https.port", "9443");
    p.setProperty("sonar.web.host", "1.2.3.4");

    TomcatConnectors.configure(tomcat, new Props(p));

    verify(tomcat.getService()).addConnector(argThat(new ArgumentMatcher<Connector>() {
      @Override
      public boolean matches(Object o) {
        Connector c = (Connector) o;
        return c.getScheme().equals("http") && c.getPort() == 9000 && ((InetAddress) c.getProperty("address")).getHostAddress().equals("1.2.3.4");
      }
    }));
    verify(tomcat.getService()).addConnector(argThat(new ArgumentMatcher<Connector>() {
      @Override
      public boolean matches(Object o) {
        Connector c = (Connector) o;
        return c.getScheme().equals("https") && c.getPort() == 9443 && ((InetAddress) c.getProperty("address")).getHostAddress().equals("1.2.3.4");
      }
    }));
  }

  @Test
  public void enable_client_auth() throws Exception {

    Properties p = new Properties();

    p.setProperty("sonar.web.port", "-1");
    p.setProperty("sonar.web.https.port", "9443");
    p.setProperty("sonar.web.https.clientAuth", "want");

    Props props = new Props(p);

    TomcatConnectors.configure(tomcat, props);

    verify(tomcat).setConnector(argThat(new ArgumentMatcher<Connector>() {
      @Override
      public boolean matches(Object o) {
        Connector c = (Connector) o;
        return c.getScheme().equals("https") && c.getProperty("clientAuth").equals("want");
      }
    }));
  }

  @Test
  public void require_client_auth() throws Exception {

    Properties p = new Properties();

    p.setProperty("sonar.web.port", "-1");
    p.setProperty("sonar.web.https.port", "9443");
    p.setProperty("sonar.web.https.clientAuth", "true");

    Props props = new Props(p);

    TomcatConnectors.configure(tomcat, props);

    verify(tomcat).setConnector(argThat(new ArgumentMatcher<Connector>() {
      @Override
      public boolean matches(Object o) {
        Connector c = (Connector) o;
        return c.getScheme().equals("https") && c.getProperty("clientAuth").equals("true");
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
