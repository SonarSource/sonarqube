/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.application;

import com.google.common.collect.ImmutableMap;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class ConnectorsTest {

  Tomcat tomcat = mock(Tomcat.class, Mockito.RETURNS_DEEP_STUBS);

  //---- connectors

  @Test
  public void configure_thread_pool() throws Exception {
    Properties p = new Properties();
    p.setProperty("sonar.web.http.minThreads", "2");
    p.setProperty("sonar.web.http.maxThreads", "30");
    p.setProperty("sonar.web.http.acceptCount", "20");
    Props props = new Props(p);

    Connectors.configure(tomcat, props);

    verify(tomcat).setConnector(argThat(new PropertiesMatcher(
      ImmutableMap.<String, Object>of("minSpareThreads", 2, "maxThreads", 30, "acceptCount", 20)
    )));
  }

  @Test
  public void configure_default_thread_pool() throws Exception {
    Props props = new Props(new Properties());

    Connectors.configure(tomcat, props);

    verify(tomcat).setConnector(argThat(new PropertiesMatcher(
      ImmutableMap.<String, Object>of("minSpareThreads", 5, "maxThreads", 50, "acceptCount", 25)
    )));
  }

  @Test
  public void fail_if_both_http_and_https_are_disabled() {
    Properties p = new Properties();
    p.setProperty("sonar.web.http.enable", "false");
    p.setProperty("sonar.web.https.enable", "false");
    Props props = new Props(p);

    try {
      Connectors.configure(tomcat, props);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("Both HTTP and HTTPS connectors are disabled");
    }
  }

  @Test
  public void only_https_is_enabled() {
    Properties p = new Properties();
    p.setProperty("sonar.web.http.enable", "false");
    p.setProperty("sonar.web.https.enable", "true");
    Props props = new Props(p);

    Connectors.configure(tomcat, props);

    verify(tomcat).setConnector(argThat(new ArgumentMatcher<Connector>() {
      @Override
      public boolean matches(Object o) {
        Connector c = (Connector)o;
        return c.getScheme().equals("https") && c.getPort()==9443;
      }
    }));
  }

  @Test
  public void both_http_and_https_are_enabled() {
    Properties p = new Properties();
    p.setProperty("sonar.web.http.enable", "true");
    p.setProperty("sonar.web.https.enable", "true");
    Props props = new Props(p);

    Connectors.configure(tomcat, props);

    verify(tomcat.getService()).addConnector(argThat(new ArgumentMatcher<Connector>() {
      @Override
      public boolean matches(Object o) {
        Connector c = (Connector)o;
        return c.getScheme().equals("http") && c.getPort()==9000;
      }
    }));
    verify(tomcat.getService()).addConnector(argThat(new ArgumentMatcher<Connector>() {
      @Override
      public boolean matches(Object o) {
        Connector c = (Connector)o;
        return c.getScheme().equals("https") && c.getPort()==9443;
      }
    }));
  }


  //---- shutdown port

  @Test
  public void enable_shutdown_port() throws Exception {
    Properties p = new Properties();
    p.setProperty("sonar.web.shutdown.port", "9010");
    p.setProperty("sonar.web.shutdown.token", "SHUTDOWN");
    Props props = new Props(p);

    Connectors.configure(tomcat, props);

    verify(tomcat.getServer()).setPort(9010);
    verify(tomcat.getServer()).setShutdown("SHUTDOWN");
  }

  @Test
  public void disable_shutdown_port_by_default() throws Exception {
    Props props = new Props(new Properties());

    Connectors.configure(tomcat, props);

    verify(tomcat.getServer(), never()).setPort(anyInt());
    verify(tomcat.getServer(), never()).setShutdown(anyString());
  }

  @Test
  public void disable_shutdown_port_if_missing_token() throws Exception {
    Properties p = new Properties();
    // only the port, but not the token
    p.setProperty("sonar.web.shutdown.port", "9010");
    Props props = new Props(p);

    Connectors.configure(tomcat, props);

    verify(tomcat.getServer(), never()).setPort(anyInt());
    verify(tomcat.getServer(), never()).setShutdown(anyString());
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
