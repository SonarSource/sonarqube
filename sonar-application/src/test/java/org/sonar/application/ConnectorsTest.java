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

import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.util.Properties;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class ConnectorsTest {
  @Test
  public void enable_shutdown_port() throws Exception {
    Properties p = new Properties();
    p.setProperty(Connectors.PROPERTY_SHUTDOWN_PORT, "9010");
    p.setProperty(Connectors.PROPERTY_SHUTDOWN_TOKEN, "SHUTDOWN");
    Props props = new Props(p);

    Tomcat tomcat = mock(Tomcat.class, Mockito.RETURNS_DEEP_STUBS);
    Connectors.configure(tomcat, props);

    verify(tomcat.getServer()).setPort(9010);
    verify(tomcat.getServer()).setShutdown("SHUTDOWN");
  }

  @Test
  public void disable_shutdown_port_by_default() throws Exception {
    Props props = new Props(new Properties());

    Tomcat tomcat = mock(Tomcat.class, Mockito.RETURNS_DEEP_STUBS);
    Connectors.configure(tomcat, props);

    verify(tomcat.getServer(), never()).setPort(anyInt());
    verify(tomcat.getServer(), never()).setShutdown(anyString());
  }

  @Test
  public void configure_thread_pool() throws Exception {
    Properties p = new Properties();
    p.setProperty(Connectors.PROPERTY_MIN_THREADS, "2");
    p.setProperty(Connectors.PROPERTY_MAX_THREADS, "30");
    p.setProperty(Connectors.PROPERTY_ACCEPT_COUNT, "20");
    Props props = new Props(p);

    Tomcat tomcat = mock(Tomcat.class, Mockito.RETURNS_DEEP_STUBS);
    Connectors.configure(tomcat, props);

    verify(tomcat).setConnector(argThat(new ArgumentMatcher<Connector>() {
      @Override
      public boolean matches(Object o) {
        Connector c = (Connector)o;
        return (Integer)c.getProperty("minSpareThreads") == 2 &&
          (Integer) c.getProperty("maxThreads") == 30 &&
          (Integer) c.getProperty("acceptCount") == 20;
      }
    }));
  }

  @Test
  public void configure_default_thread_pool() throws Exception {
    Props props = new Props(new Properties());

    Tomcat tomcat = mock(Tomcat.class, Mockito.RETURNS_DEEP_STUBS);
    Connectors.configure(tomcat, props);

    verify(tomcat).setConnector(argThat(new ArgumentMatcher<Connector>() {
      @Override
      public boolean matches(Object o) {
        Connector c = (Connector)o;
        return (Integer)c.getProperty("minSpareThreads") == 5 &&
          (Integer) c.getProperty("maxThreads") == 50 &&
          (Integer) c.getProperty("acceptCount") == 25;
      }
    }));
  }
}
